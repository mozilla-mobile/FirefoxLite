/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.privately

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.support.annotation.CheckResult
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.view.View
import android.widget.Toast
import org.mozilla.focus.BuildConfig
import org.mozilla.focus.Inject
import org.mozilla.focus.R
import org.mozilla.focus.activity.BaseActivity
import org.mozilla.focus.activity.MainActivity
import org.mozilla.focus.download.DownloadInfoManager
import org.mozilla.focus.navigation.ScreenNavigator
import org.mozilla.focus.navigation.ScreenNavigator.BrowserScreen
import org.mozilla.focus.navigation.ScreenNavigator.HomeScreen
import org.mozilla.focus.navigation.ScreenNavigator.Screen
import org.mozilla.focus.navigation.ScreenNavigator.UrlInputScreen
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.urlinput.UrlInputFragment
import org.mozilla.focus.utils.AppConstants
import org.mozilla.focus.utils.Constants
import org.mozilla.focus.utils.SafeIntent
import org.mozilla.focus.utils.ShortcutUtils
import org.mozilla.focus.utils.SupportUtils
import org.mozilla.focus.widget.FragmentListener
import org.mozilla.focus.widget.FragmentListener.TYPE
import org.mozilla.rocket.chrome.ChromeViewModel
import org.mozilla.rocket.component.LaunchIntentDispatcher
import org.mozilla.rocket.component.PrivateSessionNotificationService
import org.mozilla.rocket.landing.NavigationModel
import org.mozilla.rocket.landing.OrientationState
import org.mozilla.rocket.landing.PortraitStateModel
import org.mozilla.rocket.privately.browse.BrowserFragment
import org.mozilla.rocket.privately.home.PrivateHomeFragment
import org.mozilla.rocket.tabs.SessionManager
import org.mozilla.rocket.tabs.TabsSessionProvider

class PrivateModeActivity : BaseActivity(),
        FragmentListener,
        ScreenNavigator.Provider,
        ScreenNavigator.HostActivity,
        TabsSessionProvider.SessionHost {

    private val LOG_TAG = "PrivateModeActivity"
    private var sessionManager: SessionManager? = null
    private lateinit var chromeViewModel: ChromeViewModel
    private lateinit var tabViewProvider: PrivateTabViewProvider
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var screenNavigator: ScreenNavigator
    private lateinit var uiMessageReceiver: BroadcastReceiver
    private lateinit var snackBarContainer: View

    private val portraitStateModel = PortraitStateModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        // we don't keep any state if user leave Private-mode
        super.onCreate(null)

        chromeViewModel = Inject.obtainChromeViewModel(this)
        tabViewProvider = PrivateTabViewProvider(this)
        screenNavigator = ScreenNavigator(this)

        val exitEarly = handleIntent(intent)
        if (exitEarly) {
            pushToBack()
            return
        }

        setContentView(R.layout.activity_private_mode)

        snackBarContainer = findViewById(R.id.container)
        makeStatusBarTransparent()

        initViewModel()
        initBroadcastReceivers()

        screenNavigator.popToHomeScreen(false)
        observeChromeAction()

        monitorOrientationState()
    }

    private fun initViewModel() {
        sharedViewModel = ViewModelProviders.of(this).get(SharedViewModel::class.java)
        sharedViewModel.urlInputState().value = false
    }

    override fun onResume() {
        super.onResume()
        val uiActionFilter = IntentFilter()
        uiActionFilter.addCategory(Constants.CATEGORY_FILE_OPERATION)
        uiActionFilter.addAction(Constants.ACTION_NOTIFY_RELOCATE_FINISH)
        LocalBroadcastManager.getInstance(this).registerReceiver(uiMessageReceiver, uiActionFilter)
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(uiMessageReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPrivateMode()
        sessionManager?.destroy()
    }

    override fun applyLocale() {}

    private fun observeChromeAction() {
        // Reserve to handle more chrome actions for the bottom bar A/B testing
        chromeViewModel.showUrlInput.observe(this, Observer { url ->
            if (!supportFragmentManager.isStateSaved) {
                screenNavigator.addUrlScreen(url)
            }
        })
        chromeViewModel.pinShortcut.observe(this, Observer {
            onAddToHomeClicked()
        })
        chromeViewModel.share.observe(this, Observer {
            chromeViewModel.currentUrl.value?.let { url ->
                onShareClicked(url)
            }
        })
        chromeViewModel.togglePrivateMode.observe(this, Observer {
            checkShortcutPromotion { pushToBack() }
        })
        chromeViewModel.dropCurrentPage.observe(this, Observer {
            dropBrowserFragment()
        })
    }

    private fun onAddToHomeClicked() {
        val focusTab = getSessionManager().focusSession ?: return
        val url = focusTab.url
        // If we pin an invalid url as shortcut, the app will not function properly.
        // TODO: only enable the bottom menu item if the page is valid and loaded.
        if (!SupportUtils.isUrl(url)) {
            return
        }
        val bitmap = focusTab.favicon
        val shortcut = Intent(Intent.ACTION_VIEW)
        // Use activity-alias name here so we can start whoever want to control launching behavior
        // Besides, RocketLauncherActivity not exported so using the alias-name is required.
        shortcut.setClassName(this, AppConstants.LAUNCHER_ACTIVITY_ALIAS)
        shortcut.data = Uri.parse(url)
        shortcut.putExtra(LaunchIntentDispatcher.LaunchMethod.EXTRA_BOOL_HOME_SCREEN_SHORTCUT.value, true)

        ShortcutUtils.requestPinShortcut(this, shortcut, focusTab.title, url!!, bitmap)
    }

    private fun onShareClicked(url: String) {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, url)
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_dialog_title)))
    }

    override fun onNotified(from: Fragment, type: FragmentListener.TYPE, payload: Any?) {
        when (type) {
            TYPE.SHOW_URL_INPUT -> showUrlInput(payload)
            TYPE.DISMISS_URL_INPUT -> dismissUrlInput()
            TYPE.OPEN_URL_IN_CURRENT_TAB -> openUrl(payload)
            TYPE.OPEN_URL_IN_NEW_TAB -> openUrl(payload)
            else -> {
            }
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.isStateSaved) {
            return
        }

        val handled = screenNavigator.visibleBrowserScreen?.onBackPressed() ?: false
        if (handled) {
            return
        }

        if (!this.screenNavigator.canGoBack()) {
            checkShortcutPromotion { finish() }
            return
        }

        super.onBackPressed()
    }

    override fun getSessionManager(): SessionManager {
        if (sessionManager == null) {
            sessionManager = SessionManager(tabViewProvider)
        }

        // we just created it, it definitely not null
        return sessionManager!!
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        val exitEarly = handleIntent(intent)
        if (exitEarly) {
            return
        }

        setIntent(intent)
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        intent?.let { SafeIntent(intent) }?.let { safeIntent ->
            if (safeIntent.action == Intent.ACTION_VIEW) {
                safeIntent.dataString?.let { openUrl(it) }
            }
        }
    }

    private fun monitorOrientationState() {
        val orientationState = OrientationState(object : NavigationModel {
            override val navigationState: LiveData<ScreenNavigator.NavigationState>
                get() = ScreenNavigator.get(this@PrivateModeActivity).navigationState
        }, portraitStateModel)

        orientationState.observe(this, Observer { orientation ->
            orientation?.let {
                requestedOrientation = it
            }
        })
    }

    private fun dropBrowserFragment() {
        stopPrivateMode()
        Toast.makeText(this, R.string.private_browsing_erase_done, Toast.LENGTH_LONG).show()
    }

    override fun getScreenNavigator(): ScreenNavigator = screenNavigator

    override fun getBrowserScreen(): BrowserScreen {
        return supportFragmentManager.findFragmentById(R.id.browser) as BrowserFragment
    }

    override fun createFirstRunScreen(): Screen {
        if (BuildConfig.DEBUG) {
            throw RuntimeException("PrivateModeActivity should never show first-run")
        }
        TODO("PrivateModeActivity should never show first-run")
    }

    override fun createHomeScreen(): HomeScreen {
        return PrivateHomeFragment.create()
    }

    override fun createUrlInputScreen(url: String?, parentFragmentTag: String?): UrlInputScreen {
        return UrlInputFragment.create(url, null, false)
    }

    private fun pushToBack() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME)
        startActivity(intent)
        overridePendingTransition(0, R.anim.pb_exit)
    }

    private fun showUrlInput(payload: Any?) {
        val url = payload?.toString() ?: ""
        screenNavigator.addUrlScreen(url)
        sharedViewModel.urlInputState().value = true
    }

    private fun dismissUrlInput() {
        screenNavigator.popUrlScreen()
        sharedViewModel.urlInputState().value = false
    }

    private fun openUrl(payload: Any?) {
        val url = payload?.toString() ?: ""

        ViewModelProviders.of(this)
                .get(SharedViewModel::class.java)
                .setUrl(url)

        dismissUrlInput()
        startPrivateMode()
        ScreenNavigator.get(this).showBrowserScreen(url, false, false)
    }

    private fun makeStatusBarTransparent() {
        var visibility = window.decorView.systemUiVisibility
        // do not overwrite existing value
        visibility = visibility or (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        window.decorView.systemUiVisibility = visibility
    }

    private fun startPrivateMode() {
        PrivateSessionNotificationService.start(this)
    }

    private fun stopPrivateMode() {
        PrivateSessionNotificationService.stop(this)
        PrivateMode.sanitize(this.applicationContext)
        tabViewProvider.purify(this)
    }

    @CheckResult
    private fun handleIntent(intent: Intent?): Boolean {

        if (intent?.action == PrivateMode.INTENT_EXTRA_SANITIZE) {
            TelemetryWrapper.erasePrivateModeNotification()
            stopPrivateMode()
            Toast.makeText(this, R.string.private_browsing_erase_done, Toast.LENGTH_LONG).show()
            finishAndRemoveTask()
            return true
        }
        return false
    }

    private fun initBroadcastReceivers() {
        uiMessageReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Constants.ACTION_NOTIFY_RELOCATE_FINISH) {
                    DownloadInfoManager.getInstance().showOpenDownloadSnackBar(intent.getLongExtra(Constants.EXTRA_ROW_ID, -1), snackBarContainer, LOG_TAG)
                }
            }
        }
    }

    private fun checkShortcutPromotion(continuation: () -> Unit) {
        ViewModelProviders.of(this)
                .get(ShortcutViewModel::class.java)
                .interceptLeavingAndCheckShortcut(this)
                .observe(this, Observer {
                    continuation()
                })
    }
}
