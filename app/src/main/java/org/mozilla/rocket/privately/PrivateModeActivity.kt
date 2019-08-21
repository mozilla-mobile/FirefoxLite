/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.privately

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import dagger.Lazy
import org.mozilla.focus.BuildConfig
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
import org.mozilla.rocket.chrome.ChromeViewModel
import org.mozilla.rocket.chrome.ChromeViewModel.OpenUrlAction
import org.mozilla.rocket.component.LaunchIntentDispatcher
import org.mozilla.rocket.component.PrivateSessionNotificationService
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.getViewModel
import org.mozilla.rocket.landing.NavigationModel
import org.mozilla.rocket.landing.OrientationState
import org.mozilla.rocket.landing.PortraitStateModel
import org.mozilla.rocket.privately.browse.BrowserFragment
import org.mozilla.rocket.privately.home.PrivateHomeFragment
import org.mozilla.rocket.tabs.SessionManager
import org.mozilla.rocket.tabs.TabsSessionProvider
import javax.inject.Inject

class PrivateModeActivity : BaseActivity(),
        ScreenNavigator.Provider,
        ScreenNavigator.HostActivity,
        TabsSessionProvider.SessionHost {

    @Inject
    lateinit var chromeViewModelCreator: Lazy<ChromeViewModel>

    private val LOG_TAG = "PrivateModeActivity"
    private var sessionManager: SessionManager? = null
    private lateinit var chromeViewModel: ChromeViewModel
    private lateinit var tabViewProvider: PrivateTabViewProvider
    private lateinit var screenNavigator: ScreenNavigator
    private lateinit var uiMessageReceiver: BroadcastReceiver
    private lateinit var snackBarContainer: View

    private val portraitStateModel = PortraitStateModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        // we don't keep any state if user leave Private-mode
        appComponent().inject(this)
        super.onCreate(null)

        chromeViewModel = getViewModel(chromeViewModelCreator)
        tabViewProvider = PrivateTabViewProvider(this)
        screenNavigator = ScreenNavigator(this)

        if (isSanitizeIntent(intent)) {
            sanitize()
            pushToBack()
            return
        }

        handleIntent(intent)

        setContentView(R.layout.activity_private_mode)

        snackBarContainer = findViewById(R.id.container)
        makeStatusBarTransparent()

        initBroadcastReceivers()

        screenNavigator.popToHomeScreen(false)
        observeChromeAction()

        monitorOrientationState()
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
        chromeViewModel.openUrl.observe(this, Observer { action ->
            action?.run {
                dismissUrlInput()
                startPrivateMode()
                screenNavigator.showBrowserScreen(url, false, isFromExternal)
            }
        })
        chromeViewModel.showUrlInput.observe(this, Observer { url ->
            if (!supportFragmentManager.isStateSaved) {
                screenNavigator.addUrlScreen(url)
            }
        })
        chromeViewModel.dismissUrlInput.observe(this, Observer {
            dismissUrlInput()
        })
        // Reserve to handle more chrome actions for the bottom bar A/B testing
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

    override fun onBackPressed() {
        if (supportFragmentManager.isStateSaved) {
            return
        }

        val handled = screenNavigator.visibleBrowserScreen?.onBackPressed() ?: false
        if (handled) {
            return
        }

        if (!this.screenNavigator.canGoBack()) {
            checkShortcutPromotion {
                TelemetryWrapper.exitPrivateMode(TelemetryWrapper.Extra_Value.SYSTEM_BACK)
                finish()
            }
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

        if (isSanitizeIntent(intent)) {
            sanitize()
            return
        }

        handleIntent(intent)
        setIntent(intent)
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

    private fun dismissUrlInput() {
        screenNavigator.popUrlScreen()
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
        PrivateMode.getInstance(this).sanitize()
        tabViewProvider.purify(this)
    }

    private fun isSanitizeIntent(intent: Intent?): Boolean {
        return intent?.action == PrivateMode.INTENT_EXTRA_SANITIZE
    }

    private fun sanitize() {
        TelemetryWrapper.erasePrivateModeNotification()
        stopPrivateMode()
        Toast.makeText(this, R.string.private_browsing_erase_done, Toast.LENGTH_LONG).show()
        finishAndRemoveTask()
    }

    private fun handleIntent(intent: Intent?) {
        val safeIntent = intent?.let { SafeIntent(it) } ?: return

        when (safeIntent.action) {
            Intent.ACTION_VIEW -> onReceiveViewIntent(safeIntent)
            Intent.ACTION_MAIN -> onReceiveMainIntent(safeIntent)
        }
    }

    private fun onReceiveViewIntent(intent: SafeIntent) {
        TelemetryWrapper.launchByPrivateModeShortcut(TelemetryWrapper.Extra_Value.EXTERNAL_APP)
        val fromHistory = (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0
        if (!fromHistory) {
            intent.dataString?.let { url ->
                chromeViewModel.openUrl.value = OpenUrlAction(url, withNewTab = false, isFromExternal = true)
            }
        }
    }

    private fun onReceiveMainIntent(intent: SafeIntent) {
        if (isIntentFromPrivateShortcut(intent)) {
            TelemetryWrapper.launchByPrivateModeShortcut(TelemetryWrapper.Extra_Value.LAUNCHER)
        }
    }

    private fun isIntentFromPrivateShortcut(intent: SafeIntent): Boolean {
        return intent.getBooleanExtra(
                LaunchIntentDispatcher.LaunchMethod.EXTRA_BOOL_PRIVATE_MODE_SHORTCUT.value,
                false
        )
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

    override fun createMissionDetailScreen(): ScreenNavigator.MissionDetailScreen {
        if (BuildConfig.DEBUG) {
            throw RuntimeException("PrivateModeActivity should never show Mission Detail")
        }
        TODO("PrivateModeActivity should never show Mission Detail")
    }

    override fun createFxLoginScreen(): ScreenNavigator.FxLoginScreen {
        if (BuildConfig.DEBUG) {
            throw RuntimeException("PrivateModeActivity should never show FxLogin")
        }
        TODO("PrivateModeActivity should never show FxLogin")
    }

    override fun createRedeemScreen(): ScreenNavigator.RedeemSceen {
        if (BuildConfig.DEBUG) {
            throw RuntimeException("PrivateModeActivity should never show Redeem")
        }
        TODO("PrivateModeActivity should never show Redeem")
    }
}
