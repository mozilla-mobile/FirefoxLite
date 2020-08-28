/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus

import android.app.Activity
import android.os.Bundle
import android.os.StrictMode
import android.preference.PreferenceManager
import android.util.TimingLogger
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import mozilla.components.browser.engine.system.SystemEngine
import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.engine.DefaultSettings
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.EngineSession
import org.mozilla.focus.history.BrowsingHistoryManager
import org.mozilla.focus.locale.LocaleAwareApplication
import org.mozilla.focus.notification.NotificationUtil
import org.mozilla.focus.screenshot.ScreenshotManager
import org.mozilla.focus.search.SearchEngineManager
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.AppConstants
import org.mozilla.focus.utils.FirebaseHelper
import org.mozilla.focus.utils.Settings
import org.mozilla.focus.web.WebViewProvider
import org.mozilla.rocket.abtesting.LocalAbTesting
import org.mozilla.rocket.di.AppComponent
import org.mozilla.rocket.di.AppModule
import org.mozilla.rocket.di.DaggerAppComponent
import org.mozilla.rocket.privately.PrivateMode.Companion.PRIVATE_PROCESS_NAME
import org.mozilla.rocket.privately.PrivateMode.Companion.WEBVIEW_FOLDER_NAME
import org.mozilla.rocket.privately.PrivateModeActivity
import org.mozilla.rocket.settings.SettingsProvider
import java.io.File

open class FocusApplication : LocaleAwareApplication(), LifecycleObserver {

    private var appComponent: AppComponent? = null

    var isInPrivateProcess = false
    var isForeground = false

    val settings by lazy {
        SettingsProvider(this)
    }
    val engine: Engine by lazy {
        SystemEngine(this, engineSettings)
    }
    val engineSettings: DefaultSettings by lazy {
        DefaultSettings(
            trackingProtectionPolicy = createTrackingProtectionPolicy(isInPrivateProcess),
            displayZoomControls = false,
            // To respect the html viewport:
            loadWithOverviewMode = true,
            // Disable access to arbitrary local files by webpages - assets can still be loaded
            // via file:///android_asset/res, so at least error page images won't be blocked.
            allowFileAccess = false,
            allowFileAccessFromFileURLs = false,
            allowUniversalAccessFromFileURLs = false,
            userAgentString = WebViewProvider.getUserAgentString(this),
            // Right now I do not know why we should allow loading content from a content provider
            allowContentAccess = false,
            // The default for those settings should be "false" - But we want to be explicit.
            domStorageEnabled = true
        )
    }
    val sessionManager: SessionManager by lazy {
        SessionManager(engine)
    }

    fun getAppComponent(): AppComponent {
        if (appComponent == null) {
            synchronized(this@FocusApplication) {
                if (appComponent == null) {
                    appComponent = DaggerAppComponent.builder()
                            .appModule(AppModule(this))
                            .build()
                }
            }
        }

        return requireNotNull(appComponent)
    }

    fun resetAppComponent() {
        appComponent = null
    }

    private fun createTrackingProtectionPolicy(isPrivateMode: Boolean): EngineSession.TrackingProtectionPolicy? {
        return if (isPrivateMode) {
            if (settings.privateBrowsingSettings.shouldUseTurboMode()) {
                EngineSession.TrackingProtectionPolicy.all()
            } else {
                null
            }
        } else {
            if (Settings.getInstance(this).shouldUseTurboMode()) {
                EngineSession.TrackingProtectionPolicy.all()
            } else {
                null
            }
        }
    }

    // Override getCacheDir cause when we create a WebView, it'll asked the application's
    // getCacheDir() method and create WebView specific cache.
    override fun getCacheDir(): File {
        if (isInPrivateProcess) {
            return File(super.getCacheDir().absolutePath + "-" + PRIVATE_PROCESS_NAME)
        }
        return super.getCacheDir()
    }

    // Override getCacheDir cause when we create a WebView, it'll asked the application's
    // getDir() method and create WebView specific files.
    override fun getDir(name: String?, mode: Int): File {
        if (name == WEBVIEW_FOLDER_NAME && isInPrivateProcess) {
            return super.getDir("$name-$PRIVATE_PROCESS_NAME", mode)
        }
        return super.getDir(name, mode)
    }

    override fun onCreate() {
        TimingLogger(TAG, "coldStart before firebase performance initialization").also {
            TelemetryWrapper.init(this)
            it.addSplit("init TelemetryWrapper")
        }.dumpToLog()
        FirebaseHelper.newTrace("coldStart")?.start()

        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        PreferenceManager.setDefaultValues(this, R.xml.settings, false)

        enableStrictMode()

        SearchEngineManager.getInstance().init(this)

        LocalAbTesting.init(this)

        BrowsingHistoryManager.getInstance().init(this)
        ScreenshotManager.getInstance().init(this)
        // initialize the NotificationUtil to configure the default notification channel. This is required for API 26+
        NotificationUtil.init(this)

        monitorPrivateProcess()

        registerCustomInAppMessagingListener()
    }

    /**
     *   We use PrivateModeActivity's existence to determine if we are in private mode (process)  or not. We don't use
     *   ActivityManager.getRunningAppProcesses() cause it sometimes return null.
     *
     *   The Application class should also rely on this flag to determine if it want to override getDir() and getCacheDir().
     *
     *  Note: we can be in private mode process but don't have any private session yet. ( e.g. We launched
     *  PrivateModeActivity but haven't create any tab yet)
     *
     */
    private fun monitorPrivateProcess() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityPaused(activity: Activity?) {
            }

            override fun onActivityResumed(activity: Activity?) {
            }

            override fun onActivityStarted(activity: Activity?) {
            }

            override fun onActivityDestroyed(activity: Activity?) {
            }

            override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {
            }

            override fun onActivityStopped(activity: Activity?) {
            }

            override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {
                // once PrivateModeActivity exist, this process is for private mode
                if (activity is PrivateModeActivity) {
                    isInPrivateProcess = true
                }
            }
        })
    }

    private fun registerCustomInAppMessagingListener() {
        if (!AppConstants.isBuiltWithFirebase()) return

        FirebaseHelper.getFirebase().addIamImpressionListener { inAppMessage ->
            val campaignName = inAppMessage.campaignMetadata?.campaignName
            TelemetryWrapper.showInAppMessage(campaignName)
        }
        FirebaseHelper.getFirebase().addIamClickListener { inAppMessage, action ->
            val campaignName = inAppMessage.campaignMetadata?.campaignName
            val buttonText = action.buttonText
            val actionUrl = action.actionUrl
            TelemetryWrapper.clickInAppMessage(campaignName, buttonText, actionUrl)
        }
    }

    private fun enableStrictMode() {
        if (AppConstants.isReleaseBuild()) {
            return
        }

        val threadPolicyBuilder = StrictMode.ThreadPolicy.Builder().detectAll()
        val vmPolicyBuilder = StrictMode.VmPolicy.Builder().detectAll()

        // Provide different strict mode penalty for ui testing and production code
        if (AppConstants.isUnderEspressoTest()) {
            // In AndroidTest we are super kind :)
            threadPolicyBuilder.penaltyLog()
        } else {
            threadPolicyBuilder.penaltyLog().penaltyDialog()
        }
        vmPolicyBuilder.penaltyLog()

        StrictMode.setThreadPolicy(threadPolicyBuilder.build())
        StrictMode.setVmPolicy(vmPolicyBuilder.build())
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppInForeground() {
        isForeground = true
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppInBackground() {
        isForeground = false
    }

    companion object {
        private const val TAG = "FocusApplication"
    }
}
