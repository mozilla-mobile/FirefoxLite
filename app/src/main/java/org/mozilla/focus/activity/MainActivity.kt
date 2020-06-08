/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity

import android.app.Activity
import android.app.Dialog
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import dagger.Lazy
import kotlinx.android.synthetic.main.activity_main.container
import org.mozilla.focus.R
import org.mozilla.focus.download.DownloadInfoManager
import org.mozilla.focus.fragment.BrowserFragment
import org.mozilla.focus.fragment.FirstrunFragment
import org.mozilla.focus.fragment.ListPanelDialog
import org.mozilla.focus.navigation.ScreenNavigator
import org.mozilla.focus.notification.NotificationId
import org.mozilla.focus.notification.NotificationUtil
import org.mozilla.focus.persistence.TabModelStore
import org.mozilla.focus.provider.DownloadContract
import org.mozilla.focus.screenshot.ScreenshotGridFragment
import org.mozilla.focus.screenshot.ScreenshotViewerActivity
import org.mozilla.focus.tabs.tabtray.TabTray
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.urlinput.UrlInputFragment
import org.mozilla.focus.utils.AppConstants
import org.mozilla.focus.utils.Constants
import org.mozilla.focus.utils.DialogUtils
import org.mozilla.focus.utils.FirebaseHelper
import org.mozilla.focus.utils.FirebaseHelper.FIREBASE_READY
import org.mozilla.focus.utils.IntentUtils
import org.mozilla.focus.utils.SafeIntent
import org.mozilla.focus.utils.ShortcutUtils
import org.mozilla.focus.utils.SupportUtils
import org.mozilla.focus.web.GeoPermissionCache
import org.mozilla.focus.web.WebViewProvider
import org.mozilla.rocket.appupdate.InAppUpdateController
import org.mozilla.rocket.appupdate.InAppUpdateIntro
import org.mozilla.rocket.chrome.ChromeViewModel
import org.mozilla.rocket.chrome.ChromeViewModel.OpenUrlAction
import org.mozilla.rocket.component.LaunchIntentDispatcher
import org.mozilla.rocket.component.PrivateSessionNotificationService
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.getViewModel
import org.mozilla.rocket.download.DownloadIndicatorViewModel
import org.mozilla.rocket.extension.nonNullObserve
import org.mozilla.rocket.home.HomeFragment
import org.mozilla.rocket.landing.DialogQueue
import org.mozilla.rocket.landing.NavigationModel
import org.mozilla.rocket.landing.OrientationState
import org.mozilla.rocket.landing.PortraitComponent
import org.mozilla.rocket.landing.PortraitStateModel
import org.mozilla.rocket.menu.MenuDialog
import org.mozilla.rocket.periodic.FirstLaunchWorker
import org.mozilla.rocket.periodic.PeriodicReceiver
import org.mozilla.rocket.privately.PrivateMode
import org.mozilla.rocket.privately.PrivateModeActivity
import org.mozilla.rocket.promotion.PromotionModel
import org.mozilla.rocket.promotion.PromotionPresenter
import org.mozilla.rocket.promotion.PromotionViewContract
import org.mozilla.rocket.settings.defaultbrowser.data.DefaultBrowserRepository
import org.mozilla.rocket.tabs.SessionManager
import org.mozilla.rocket.tabs.TabView
import org.mozilla.rocket.tabs.TabViewProvider
import org.mozilla.rocket.tabs.TabsSessionProvider
import org.mozilla.rocket.theme.ThemeManager
import org.mozilla.rocket.widget.enqueue
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class MainActivity : BaseActivity(),
        ThemeManager.ThemeHost,
        TabsSessionProvider.SessionHost,
        ScreenNavigator.Provider,
        ScreenNavigator.HostActivity,
        PromotionViewContract,
        InAppUpdateController.ViewDelegate {

    @Inject
    lateinit var downloadIndicatorViewModelCreator: Lazy<DownloadIndicatorViewModel>
    @Inject
    lateinit var chromeViewModelCreator: Lazy<ChromeViewModel>
    @Inject
    lateinit var tabModelStore: TabModelStore
    @Inject
    lateinit var defaultBrowserRepository: DefaultBrowserRepository

    val portraitStateModel = PortraitStateModel()
    private lateinit var chromeViewModel: ChromeViewModel
    private lateinit var downloadIndicatorViewModel: DownloadIndicatorViewModel
    private var promotionModel: PromotionModel? = null

    private lateinit var menu: MenuDialog
    private var mDialogFragment: DialogFragment? = null
    private var myshotOnBoardingDialog: Dialog? = null

    private lateinit var screenNavigator: ScreenNavigator
    private lateinit var firebaseEventReceiver: BroadcastReceiver
    private lateinit var uiMessageReceiver: BroadcastReceiver

    private lateinit var appUpdateController: InAppUpdateController

    private var themeManager: ThemeManager? = null
    private var sessionManager: SessionManager? = null
    private val dialogQueue = DialogQueue()
    private var exitToast: Toast? = null

    private val downloadObserver = object : ContentObserver(null) {
        override fun onChange(selfChange: Boolean) {
            downloadIndicatorViewModel.updateIndicator()
        }
    }

    private val asyncQueryListener = TabModelStore.AsyncQueryListener { states, currentTabId ->
        chromeViewModel.onRestoreTabCountCompleted()
        getSessionManager().restore(states, currentTabId)
        val currentTab = getSessionManager().focusSession
        if (currentTab != null && !chromeViewModel.shouldShowFirstrun && !supportFragmentManager.isStateSaved) {
            screenNavigator.restoreBrowserScreen(currentTab.id)
        }
    }

    private val surveyUrl: String
        get() {
            val currentLang = Locale.getDefault().language
            val indonesiaLang = Locale("id").language
            val isSameLang = currentLang.equals(indonesiaLang, ignoreCase = true)

            return getString(R.string.survey_notification_url, if (isSameLang) "id" else "en")
        }

    @VisibleForTesting
    val visibleBrowserFragment: BrowserFragment?
        get() = if (screenNavigator.isBrowserInForeground) browserFragment else null

    @VisibleForTesting
    val browserFragment: BrowserFragment?
        get() = supportFragmentManager.findFragmentById(R.id.browser) as BrowserFragment?

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)

        FirebaseHelper.initUserState(this)

        chromeViewModel = getViewModel(chromeViewModelCreator)
        downloadIndicatorViewModel = getViewModel(downloadIndicatorViewModelCreator)
        themeManager = ThemeManager(this)
        screenNavigator = ScreenNavigator(this)
        appUpdateController = InAppUpdateController(
                this,
                AppUpdateManagerFactory.create(this),
                this
        )

        setContentView(R.layout.activity_main)
        initViews()
        initBroadcastReceivers()
        registerFirebaseEventReceiver()

        val intent = SafeIntent(intent)
        if (savedInstanceState == null) {
            val handledExternalLink = handleExternalLink(intent)
            if (!handledExternalLink) {
                if (chromeViewModel.shouldShowFirstrun) {
                    screenNavigator.addFirstRunScreen()
                } else {
                    screenNavigator.popToHomeScreen(false)
                }
            }
        }
        if (chromeViewModel.shouldShowFirstrun) {
            themeManager?.resetDefaultTheme()
        }
        restoreTabsFromPersistence()
        WebViewProvider.preload(this)

        promotionModel = PromotionModel(this, intent).also {
            checkAndRunPromotion(it)
        }
        observeNavigation()
        monitorOrientationState()
        observeChromeAction()

        appUpdateController.onReceiveIntent(getIntent())
    }

    private fun initViews() {
        var visibility = window.decorView.systemUiVisibility
        // do not overwrite existing value
        visibility = visibility or (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        window.decorView.systemUiVisibility = visibility
        setUpMenu()
    }

    private fun setUpMenu() {
        menu = MenuDialog(this, R.style.BottomSheetTheme).apply {
            setCanceledOnTouchOutside(true)
            setOnShowListener { portraitStateModel.request(PortraitComponent.BottomMenu) }
            setOnDismissListener { portraitStateModel.cancelRequest(PortraitComponent.BottomMenu) }
        }
    }

    private fun initBroadcastReceivers() {
        firebaseEventReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    FIREBASE_READY -> checkFirstrunNotification()
                }
            }
        }
        uiMessageReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Constants.ACTION_NOTIFY_UI -> {
                        val msg = intent.getCharSequenceExtra(Constants.EXTRA_MESSAGE)
                        if (!msg.isNullOrEmpty()) {
                            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                    Constants.ACTION_NOTIFY_RELOCATE_FINISH -> DownloadInfoManager.getInstance().showOpenDownloadSnackBar(intent.getLongExtra(Constants.EXTRA_ROW_ID, -1), container, LOG_TAG)
                }
            }
        }
    }

    private fun checkFirstrunNotification() {
        if (!FirstLaunchWorker.isNotificationFired(this)) {
            sendBroadcast(Intent(this, PeriodicReceiver::class.java).apply {
                action = FirstLaunchWorker.ACTION
            })
        }
    }

    private fun registerFirebaseEventReceiver() {
        LocalBroadcastManager.getInstance(this).registerReceiver(firebaseEventReceiver,
                IntentFilter(FIREBASE_READY))
    }

    private fun unregisterFirebaseEventReceiver() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(firebaseEventReceiver)
    }

    private fun checkAndRunPromotion(promotionModel: PromotionModel) {
        if (shouldRunPromotion) {
            shouldRunPromotion = false
            PromotionPresenter.runPromotion(this, promotionModel)
        }
    }

    private fun observeNavigation() {
        screenNavigator.navigationState.observe(this, Observer { state -> chromeViewModel.navigationState.setValue(state) })
    }

    private fun monitorOrientationState() {
        val orientationState = OrientationState(
                object : NavigationModel {
                    override val navigationState: LiveData<ScreenNavigator.NavigationState>
                        get() = screenNavigator.navigationState
                }, portraitStateModel
        )

        orientationState.observe(this, Observer { orientation ->
            if (orientation != null) {
                requestedOrientation = orientation
            }
        })
    }

    private fun observeChromeAction() {
        chromeViewModel.run {
            showToast.nonNullObserve(this@MainActivity) { message ->
                Toast.makeText(this@MainActivity, getString(message.stringResId!!, *message.args), message.duration).show()
            }
            openUrl.nonNullObserve(this@MainActivity) { action ->
                screenNavigator.showBrowserScreen(action.url, action.withNewTab, action.isFromExternal)
            }
            showTabTray.observe(this@MainActivity, Observer {
                TabTray.show(supportFragmentManager)
            })
            showMenu.observe(this@MainActivity, Observer { menu.show() })
            showNewTab.observe(this@MainActivity, Observer {
                screenNavigator.addHomeScreen(true)
            })
            showUrlInput.observe(this@MainActivity, Observer { url ->
                if (!supportFragmentManager.isStateSaved) {
                    screenNavigator.addUrlScreen(url)
                }
            })
            dismissUrlInput.observe(this@MainActivity, Observer { screenNavigator.popUrlScreen() })
            pinShortcut.observe(this@MainActivity, Observer { requestPinShortcut() })
            bookmarkAdded.nonNullObserve(this@MainActivity) { itemId -> showBookmarkAddedSnackbar(itemId) }
            share.observe(this@MainActivity, Observer {
                visibleBrowserFragment?.let { shareText(it.url) }
            })
            showDownloadPanel.observe(this@MainActivity, Observer { showListPanel(ListPanelDialog.TYPE_DOWNLOADS) })
            isMyShotOnBoardingPending.nonNullObserve(this@MainActivity) { isPending ->
                if (isPending) {
                    this@MainActivity.showMyShotOnBoarding()
                }
            }
            showNightModeOnBoarding.observe(this@MainActivity, Observer { showNightModeOnBoarding() })
            isNightMode.nonNullObserve(this@MainActivity) { nightModeSettings ->
                onNightModeEnabled(nightModeSettings.brightness, nightModeSettings.isEnabled)
            }
            driveDefaultBrowser.observe(this@MainActivity, Observer { driveDefaultBrowser() })
            exitApp.observe(this@MainActivity, Observer { exitApp() })
            openPreference.observe(this@MainActivity, Observer { openPreferences() })
            showBookmarks.observe(this@MainActivity, Observer { showListPanel(ListPanelDialog.TYPE_BOOKMARKS) })
            showHistory.observe(this@MainActivity, Observer { showListPanel(ListPanelDialog.TYPE_HISTORY) })
            showScreenshots.observe(this@MainActivity, Observer { showListPanel(ListPanelDialog.TYPE_SCREENSHOTS) })
            togglePrivateMode.observe(this@MainActivity, Observer { openPrivateMode() })
        }
    }

    override fun onStart() {
        checkInAppUpdate()
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        TelemetryWrapper.startSession()

        val uiActionFilter = IntentFilter(Constants.ACTION_NOTIFY_UI).apply {
            addCategory(Constants.CATEGORY_FILE_OPERATION)
            addAction(Constants.ACTION_NOTIFY_RELOCATE_FINISH)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(uiMessageReceiver, uiActionFilter)
        contentResolver.registerContentObserver(DownloadContract.Download.CONTENT_URI, true, downloadObserver)
        downloadIndicatorViewModel.updateIndicator()
        chromeViewModel.checkIfPrivateBrowsingActive()
        chromeViewModel.onSessionStarted()

        checkHasSetDefaultBrowserInProgress()
    }

    private fun checkHasSetDefaultBrowserInProgress() {
        if (defaultBrowserRepository.hasSetDefaultBrowserInProgress()) {
            defaultBrowserRepository.setDefaultBrowserInProgress(false)
            if (defaultBrowserRepository.isDefaultBrowser()) {
                return
            }
            val rootView = findViewById<ViewGroup>(android.R.id.content).getChildAt(0) as ViewGroup
            val failMessageText = getString(R.string.message_set_default_incomplet, getString(R.string.app_name))
            Snackbar.make(rootView, failMessageText, TimeUnit.SECONDS.toMillis(8).toInt())
                .setAction(R.string.private_browsing_dialog_add_shortcut_yes) {
                    startActivity(IntentUtils.createSetDefaultBrowserIntent(this))
                    TelemetryWrapper.clickSetDefaultTryAgainSnackBar()
                }.show()
            TelemetryWrapper.showSetDefaultTryAgainSnackbar()
        }
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(uiMessageReceiver)
        contentResolver.unregisterContentObserver(downloadObserver)

        TelemetryWrapper.stopSession()
        saveTabsToPersistence()
        chromeViewModel.onSessionEnded()
    }

    override fun onStop() {
        super.onStop()
        TelemetryWrapper.stopMainActivity()
    }

    public override fun onDestroy() {
        sessionManager?.destroy()
        unregisterFirebaseEventReceiver()
        super.onDestroy()
    }

    override fun onNewIntent(unsafeIntent: Intent) {
        super.onNewIntent(unsafeIntent)
        val intent = SafeIntent(unsafeIntent)
        promotionModel?.let {
            it.parseIntent(intent)
            if (PromotionPresenter.runPromotionFromIntent(this, it)) {
                // Don't run other promotion or other action if we already displayed above promotion
                return@onNewIntent
            }
        }
        val handledExternalLink = handleExternalLink(intent)
        if (handledExternalLink) {
            // We don't want to see any menu is visible when processing open url request from Intent.ACTION_VIEW
            dismissAllMenus()
            TabTray.dismiss(supportFragmentManager)
        }

        // We do not care about the previous intent anymore. But let's remember this one.
        setIntent(unsafeIntent)

        appUpdateController.onReceiveIntent(getIntent())
    }

    private fun handleExternalLink(intent: SafeIntent): Boolean {
        var handled = false
        if (Intent.ACTION_VIEW == intent.action) {
            val url = intent.dataString
            val nonNullUrl = url ?: ""
            val openInNewTab = intent.getBooleanExtra(IntentUtils.EXTRA_OPEN_NEW_TAB, true)
            chromeViewModel.openUrl.value = OpenUrlAction(nonNullUrl, openInNewTab, true)
            handled = true
        }

        return handled
    }

    override fun applyLocale() {
        // re-create bottom sheet menu
        setUpMenu()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ScreenshotViewerActivity.REQ_CODE_VIEW_SCREENSHOT) {
            if (resultCode == ScreenshotViewerActivity.RESULT_NOTIFY_SCREENSHOT_IS_DELETED) {
                Toast.makeText(this, R.string.message_deleted_screenshot, Toast.LENGTH_SHORT).show()
                mDialogFragment?.let {
                    val fragment = it.childFragmentManager.findFragmentById(R.id.main_content)
                    if (fragment is ScreenshotGridFragment && data != null) {
                        val id = data.getLongExtra(ScreenshotViewerActivity.EXTRA_SCREENSHOT_ITEM_ID, -1)
                        fragment.notifyItemDelete(id)
                    }
                }
            } else if (resultCode == ScreenshotViewerActivity.RESULT_OPEN_URL) {
                if (data != null) {
                    val url = data.getStringExtra(ScreenshotViewerActivity.EXTRA_URL)
                    mDialogFragment?.dismissAllowingStateLoss()
                    screenNavigator.showBrowserScreen(url, true, false)
                }
            }
        }

        appUpdateController.onActivityResult(requestCode, resultCode)
    }

    override fun onBackPressed() {
        when {
            supportFragmentManager.isStateSaved -> return
            screenNavigator.visibleBrowserScreen?.onBackPressed() == true -> return
            !screenNavigator.canGoBack() -> {
                if (consumeByExitToast().not()) {
                    exitToast?.cancel()
                    finish()
                }
            }
            else -> super.onBackPressed()
        }
    }

    private fun consumeByExitToast(): Boolean {
        var handled = false
        val isToastShowing = exitToast?.view?.windowToken != null
        if (!isToastShowing) {
            Toast.makeText(this, R.string.message_exit_app, Toast.LENGTH_LONG)
                    .also { exitToast = it }
                    .show()
            TelemetryWrapper.showExitToast()
            handled = true
        }

        return handled
    }

    private fun saveTabsToPersistence() {
        if (chromeViewModel.isTabRestoredComplete.value != true) {
            return
        }

        val sessions = getSessionManager().getTabs()
        for (s in sessions) {
            s.engineSession?.saveState()
        }

        val currentTabId = getSessionManager().focusSession?.id
        tabModelStore.saveTabs(this, sessions, currentTabId, null)
    }

    private fun restoreTabsFromPersistence() {
        chromeViewModel.onRestoreTabCountStarted()
        tabModelStore.getSavedTabs(this, asyncQueryListener)
    }

    override fun getThemeManager(): ThemeManager? = themeManager

    override fun getTheme(): Resources.Theme {
        val theme = super.getTheme()

        //  Oppo with android 5.1 call getTheme before activity onCreate invoked.
        //  So themeManager is not initialized and cause NPE
        themeManager?.applyCurrentTheme(theme)

        return theme
    }

    override fun postSurveyNotification() {
        val intent = IntentUtils.createInternalOpenUrlIntent(this,
                surveyUrl, true)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT)

        val builder = NotificationUtil.importantBuilder(this)
                .setContentTitle(getString(R.string.survey_notification_title, "\uD83D\uDE4C"))
                .setContentText(getString(R.string.survey_notification_description))
                .setStyle(NotificationCompat.BigTextStyle().bigText(
                        getString(R.string.survey_notification_description)))
                .setContentIntent(pendingIntent)

        NotificationUtil.sendNotification(this, NotificationId.SURVEY_ON_3RD_LAUNCH, builder)
    }

    private fun showListPanel(type: Int) {
        val dialogFragment = ListPanelDialog.newInstance(type).apply {
            isCancelable = true
            setOnDismissListener { portraitStateModel.cancelRequest(PortraitComponent.ListPanelDialog) }
        }
        portraitStateModel.request(PortraitComponent.ListPanelDialog)
        dialogFragment.show(supportFragmentManager, "")
        mDialogFragment = dialogFragment
    }

    private fun dismissAllMenus() {
        menu.dismiss()
        visibleBrowserFragment?.run {
            dismissWebContextMenu()
            dismissGeoDialog()
        }
        mDialogFragment?.dismissAllowingStateLoss()
        myshotOnBoardingDialog?.run {
            dismiss()
            myshotOnBoardingDialog = null
        }
    }

    private fun driveDefaultBrowser() {
        DialogUtils.showDefaultSettingNotification(this)
        TelemetryWrapper.showDefaultSettingNotification()
    }

    private fun openPrivateMode() {
        startActivity(PrivateModeActivity.getStartIntent(this))
        overridePendingTransition(R.anim.tab_transition_fade_in, R.anim.tab_transition_fade_out)
    }

    private fun exitApp() {
        GeoPermissionCache.clear()
        if (PrivateMode.getInstance(this).hasPrivateSession()) {
            val intent = PrivateSessionNotificationService.buildIntent(this.applicationContext, true)
            startActivity(intent)
        }
        finish()
    }

    private fun applyNightModeBrightness(enable: Boolean, brightness: Float, window: Window) {
        val layoutParams = window.attributes
        layoutParams.screenBrightness = if (enable) {
            brightness
        } else {
            // Disable night mode, restore the screen brightness
            BRIGHTNESS_OVERRIDE_NONE
        }
        window.attributes = layoutParams
    }

    private fun onNightModeEnabled(brightness: Float, enabled: Boolean) {
        applyNightModeBrightness(enabled, brightness, window)
    }

    private fun showBookmarkAddedSnackbar(bookmarkItemId: String) {
        Snackbar.make(container, R.string.bookmark_saved, Snackbar.LENGTH_LONG).apply {
            setAction(R.string.bookmark_saved_edit) {
                startActivity(Intent(this@MainActivity, EditBookmarkActivity::class.java).putExtra(ITEM_UUID_KEY, bookmarkItemId))
            }
        }.show()
    }

    private fun shareText(url: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_dialog_title)))
    }

    private fun requestPinShortcut() {
        val focusTab = getSessionManager().focusSession ?: return
        val url = focusTab.url ?: return
        // If we pin an invalid url as shortcut, the app will not function properly.
        // TODO: only enable the bottom menu item if the page is valid and loaded.
        if (!SupportUtils.isUrl(url)) {
            return
        }
        val bitmap = focusTab.favicon
        val shortcut = Intent(Intent.ACTION_VIEW).apply {
            // Use activity-alias name here so we can start whoever want to control launching behavior
            // Besides, RocketLauncherActivity not exported so using the alias-name is required.
            setClassName(this@MainActivity, AppConstants.LAUNCHER_ACTIVITY_ALIAS)
            data = Uri.parse(url)
            putExtra(LaunchIntentDispatcher.LaunchMethod.EXTRA_BOOL_HOME_SCREEN_SHORTCUT.value, true)
        }
        ShortcutUtils.requestPinShortcut(this, shortcut, focusTab.title, url, bitmap)
    }

    fun firstrunFinished() {
        screenNavigator.popToHomeScreen(false)
    }

    override fun getScreenNavigator(): ScreenNavigator = screenNavigator

    override fun createFirstRunScreen(): FirstrunFragment = FirstrunFragment.create()

    override fun getBrowserScreen(): BrowserFragment =
            supportFragmentManager.findFragmentById(R.id.browser) as BrowserFragment

    override fun createUrlInputScreen(url: String?, parentFragmentTag: String): UrlInputFragment =
            UrlInputFragment.create(url, parentFragmentTag, true)

    override fun createHomeScreen(): ScreenNavigator.HomeScreen = HomeFragment()

    override fun getSessionManager(): SessionManager =
            // TODO: Find a proper place to allocate and init SessionManager
            sessionManager.takeIf { it != null } ?: SessionManager(MainTabViewProvider(this)).also {
                sessionManager = it
            }

    override fun onPointerCaptureChanged(hasCapture: Boolean) {
        chromeViewModel.onSurveyNotificationPosted()
    }

    override fun showRateAppDialog() {
        val dialog = DialogUtils.createRateAppDialog(this)
        dialogQueue.enqueue(dialog) { TelemetryWrapper.showRateApp(false) }
    }

    override fun showRateAppNotification() {
        DialogUtils.showRateAppNotification(this)
        TelemetryWrapper.showRateApp(true)
    }

    override fun showShareAppDialog() {
        val dialog = DialogUtils.createShareAppDialog(this)
        dialogQueue.enqueue(dialog) { TelemetryWrapper.showPromoteShareDialog() }
    }

    override fun showPrivacyPolicyUpdateNotification() {
        DialogUtils.showPrivacyPolicyUpdateNotification(this)
    }

    override fun showRateAppDialogFromIntent() {
        val dialog = DialogUtils.createRateAppDialog(this)
        dialogQueue.enqueue(dialog) { TelemetryWrapper.showRateApp(false) }

        NotificationManagerCompat.from(this).cancel(NotificationId.LOVE_FIREFOX)

        // Reset extra after dialog displayed.
        intent.extras?.putBoolean(IntentUtils.EXTRA_SHOW_RATE_DIALOG, false)
    }

    private fun showNightModeOnBoarding() {
        val view = menu.findViewById<View>(R.id.menu_night_mode)
        view?.post {
            DialogUtils.showNightModeBrightnessSpotlight(
                    this@MainActivity,
                    view,
                    DialogInterface.OnCancelListener {})
        }
    }

    @VisibleForTesting
    @UiThread
    fun showMyShotOnBoarding() {
        val view = menu.findViewById<View>(R.id.menu_screenshots)
        view?.post {
            myshotOnBoardingDialog = DialogUtils.showMyShotOnBoarding(
                    this@MainActivity,
                    view,
                    DialogInterface.OnCancelListener { dismissAllMenus() },
                    View.OnClickListener {
                        val url = SupportUtils.getSumoURLForTopic(this@MainActivity, "screenshot-telemetry")
                        screenNavigator.showBrowserScreen(url, true, false)
                        dismissAllMenus()
                    })
            chromeViewModel.onMyShotOnBoardingDisplayed()
        }
        menu.show()
    }

    private fun checkInAppUpdate() {
        appUpdateController.checkUpdate()
    }

    override fun showIntroDialog(
        data: InAppUpdateIntro,
        positiveCallback: () -> Unit,
        negativeCallback: () -> Unit
    ): Boolean {

        val dialog = AlertDialog.Builder(this@MainActivity)
                .setTitle(data.title)
                .setMessage(data.description)
                .setPositiveButton(data.positiveText) { _, _ ->
                    positiveCallback.invoke()
                }
                .setNegativeButton(data.negativeText) { _, _ ->
                    negativeCallback.invoke()
                }
                .setCancelable(false)
                .create()
        dialog.setCanceledOnTouchOutside(false)
        dialogQueue.tryShow(object : DialogQueue.DialogDelegate {
            override fun setOnDismissListener(listener: () -> Unit) {
                dialog.setOnDismissListener {
                    listener()
                }
            }

            override fun show() {
                dialog.show()
            }
        })
        return true
    }

    override fun showInstallPrompt(actionCallback: () -> Unit) {
        Snackbar.make(
                container,
                getString(R.string.update_to_latest_app_snack_bar_message),
                Snackbar.LENGTH_LONG
        ).setAction(getString(R.string.update_to_latest_app_snack_bar_update)) {
            actionCallback.invoke()
        }.show()
    }

    override fun showInstallPromptNotification() {
        postInstallPromptNotification()
    }

    override fun showDownloadStartHint() {
        postDownloadingNotification()

        Toast.makeText(
                this@MainActivity,
                getString(R.string.update_to_latest_app_toast),
                Toast.LENGTH_SHORT
        ).show()
    }

    private fun postInstallPromptNotification() {
        val intent = Intent(ACTION_INSTALL_IN_APP_UPDATE).apply {
            setClassName(this@MainActivity, AppConstants.LAUNCHER_ACTIVITY_ALIAS)
        }

        val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationUtil.baseBuilder(this, NotificationUtil.Channel.LOW_PRIORITY)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentTitle(getString(R.string.update_to_latest_app_notification))
                .setLargeIcon(null)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

        NotificationUtil.sendNotification(this, NotificationId.IN_APP_UPDATE, builder)
    }

    private fun postDownloadingNotification() {
        val builder = NotificationUtil.baseBuilder(this, NotificationUtil.Channel.LOW_PRIORITY)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentTitle(getString(R.string.update_to_latest_app_toast))
                .setAutoCancel(true)

        NotificationUtil.sendNotification(this, NotificationId.IN_APP_UPDATE, builder)
    }

    // a TabViewProvider and it should only be used in this activity
    private class MainTabViewProvider internal constructor(private val activity: Activity) : TabViewProvider() {

        override fun create(): TabView {
            // FIXME: we should avoid casting here.
            // TabView and View is totally different, we know WebViewProvider returns a TabView for now,
            // but there is no promise about this.
            return WebViewProvider.create(this.activity, null) as TabView
        }
    }

    companion object {
        private const val LOG_TAG = "MainActivity"

        const val REQUEST_CODE_IN_APP_UPDATE = 1024
        const val ACTION_INSTALL_IN_APP_UPDATE = "action_install_in_app_update"

        @JvmField
        var shouldRunPromotion = true
    }
}
