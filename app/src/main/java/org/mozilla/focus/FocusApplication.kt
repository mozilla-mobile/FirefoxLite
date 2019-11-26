/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.StrictMode
import android.preference.PreferenceManager
import org.mozilla.focus.download.DownloadInfoManager
import org.mozilla.focus.history.BrowsingHistoryManager
import org.mozilla.focus.locale.LocaleAwareApplication
import org.mozilla.focus.notification.NotificationUtil
import org.mozilla.focus.screenshot.ScreenshotManager
import org.mozilla.focus.search.SearchEngineManager
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.AdjustHelper
import org.mozilla.focus.utils.AppConstants
import org.mozilla.rocket.di.AppComponent
import org.mozilla.rocket.di.AppModule
import org.mozilla.rocket.di.DaggerAppComponent
import org.mozilla.rocket.partner.PartnerActivator
import org.mozilla.rocket.privately.PrivateMode.Companion.PRIVATE_PROCESS_NAME
import org.mozilla.rocket.privately.PrivateMode.Companion.WEBVIEW_FOLDER_NAME
import org.mozilla.rocket.privately.PrivateModeActivity
import org.mozilla.rocket.settings.SettingsProvider
import java.io.File

open class FocusApplication : LocaleAwareApplication() {

    private var appComponent: AppComponent? = null

    lateinit var partnerActivator: PartnerActivator
    var isInPrivateProcess = false

    val settings by lazy {
        SettingsProvider(this)
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
        super.onCreate()

        context = this

        PreferenceManager.setDefaultValues(this, R.xml.settings, false)

        enableStrictMode()

        SearchEngineManager.getInstance().init(this)

        TelemetryWrapper.init(this)
        AdjustHelper.setupAdjustIfNeeded(this)

        BrowsingHistoryManager.getInstance().init(this)
        ScreenshotManager.getInstance().init(this)
        DownloadInfoManager.getInstance()
        DownloadInfoManager.init(this)
        // initialize the NotificationUtil to configure the default notification channel. This is required for API 26+
        NotificationUtil.init(this)

        partnerActivator = PartnerActivator(this)
        partnerActivator.launch()

        monitorPrivateProcess()
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

    companion object {

        private var context: Context? = null

        @JvmStatic
        fun getGlobalContext() = context
    }
}
