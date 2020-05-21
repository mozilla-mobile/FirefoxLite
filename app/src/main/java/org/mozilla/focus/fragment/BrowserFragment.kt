/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.focus.fragment

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.TransitionDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewStub
import android.view.WindowInsets
import android.webkit.GeolocationPermissions
import android.webkit.ValueCallback
import android.webkit.WebChromeClient.FileChooserParams
import android.webkit.WebView
import android.widget.Button
import android.widget.CheckedTextView
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.snackbar.Snackbar
import dagger.Lazy
import kotlinx.android.synthetic.main.activity_content_tab.appbar
import kotlinx.android.synthetic.main.fragment_browser.browser_bottom_bar
import kotlinx.android.synthetic.main.fragment_browser.inset_cover
import kotlinx.android.synthetic.main.fragment_browser.main_content
import kotlinx.android.synthetic.main.fragment_browser.progress_bar
import kotlinx.android.synthetic.main.fragment_browser.url_bar_divider
import kotlinx.android.synthetic.main.fragment_browser.urlbar
import kotlinx.android.synthetic.main.fragment_browser.video_container
import kotlinx.android.synthetic.main.fragment_browser.view.shopping_search_stub
import kotlinx.android.synthetic.main.fragment_browser.webview_container
import kotlinx.android.synthetic.main.fragment_browser.webview_slot
import kotlinx.android.synthetic.main.toolbar.display_url
import kotlinx.android.synthetic.main.toolbar.site_identity
import kotlinx.android.synthetic.main.toolbar.toolbar_root
import mozilla.components.browser.session.Session.FindResult
import org.mozilla.focus.BuildConfig
import org.mozilla.focus.R
import org.mozilla.focus.activity.MainActivity
import org.mozilla.focus.download.EnqueueDownloadTask
import org.mozilla.focus.locale.LocaleAwareFragment
import org.mozilla.focus.menu.WebContextMenu
import org.mozilla.focus.navigation.ScreenNavigator
import org.mozilla.focus.navigation.ScreenNavigator.BrowserScreen
import org.mozilla.focus.screenshot.CaptureRunnable
import org.mozilla.focus.screenshot.CaptureRunnable.CaptureStateListener
import org.mozilla.focus.tabs.tabtray.TabTray
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.telemetry.TelemetryWrapper.Extra_Value
import org.mozilla.focus.telemetry.TelemetryWrapper.longPressDownloadIndicator
import org.mozilla.focus.utils.AppConstants
import org.mozilla.focus.utils.FileChooseAction
import org.mozilla.focus.utils.IntentUtils
import org.mozilla.focus.utils.Settings
import org.mozilla.focus.utils.SupportUtils
import org.mozilla.focus.utils.ViewUtils
import org.mozilla.focus.viewmodel.ShoppingSearchPromptViewModel
import org.mozilla.focus.viewmodel.ShoppingSearchPromptViewModel.VisibilityState
import org.mozilla.focus.viewmodel.ShoppingSearchPromptViewModel.VisibilityState.Expanded
import org.mozilla.focus.web.GeoPermissionCache
import org.mozilla.focus.web.HttpAuthenticationDialogBuilder
import org.mozilla.focus.widget.BackKeyHandleable
import org.mozilla.focus.widget.FindInPage
import org.mozilla.permissionhandler.PermissionHandle
import org.mozilla.permissionhandler.PermissionHandler
import org.mozilla.rocket.chrome.BottomBarItemAdapter
import org.mozilla.rocket.chrome.BottomBarViewModel
import org.mozilla.rocket.chrome.ChromeViewModel
import org.mozilla.rocket.chrome.ChromeViewModel.ScreenCaptureTelemetryData
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.getActivityViewModel
import org.mozilla.rocket.content.view.BottomBar
import org.mozilla.rocket.content.view.BottomBar.BottomBarBehavior.Companion.slideUp
import org.mozilla.rocket.download.DownloadIndicatorIntroViewHelper.OnViewInflated
import org.mozilla.rocket.download.DownloadIndicatorIntroViewHelper.initDownloadIndicatorIntroView
import org.mozilla.rocket.download.DownloadIndicatorViewModel
import org.mozilla.rocket.extension.switchFrom
import org.mozilla.rocket.landing.PortraitComponent
import org.mozilla.rocket.landing.PortraitStateModel
import org.mozilla.rocket.nightmode.themed.ThemedCoordinatorLayout
import org.mozilla.rocket.shopping.search.ui.ShoppingSearchActivity.Companion.getStartIntent
import org.mozilla.rocket.shopping.search.ui.adapter.ShoppingSiteItem
import org.mozilla.rocket.tabs.Session
import org.mozilla.rocket.tabs.SessionManager
import org.mozilla.rocket.tabs.SessionManager.Factor
import org.mozilla.rocket.tabs.TabView
import org.mozilla.rocket.tabs.TabView.FullscreenCallback
import org.mozilla.rocket.tabs.TabView.HitTarget
import org.mozilla.rocket.tabs.TabViewClient.HttpAuthCallback
import org.mozilla.rocket.tabs.TabViewEngineSession
import org.mozilla.rocket.tabs.TabsSessionProvider
import org.mozilla.rocket.tabs.utils.TabUtil
import org.mozilla.rocket.tabs.web.Download
import org.mozilla.threadutils.ThreadUtils
import org.mozilla.urlutils.UrlUtils
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import javax.inject.Inject

/**
 * Fragment for displaying the browser UI.
 */
class BrowserFragment : LocaleAwareFragment(), BrowserScreen, LifecycleOwner, BackKeyHandleable {

    @Inject
    lateinit var downloadIndicatorViewModelCreator: Lazy<DownloadIndicatorViewModel>
    @Inject
    lateinit var bottomBarViewModelCreator: Lazy<BottomBarViewModel>
    @Inject
    lateinit var chromeViewModelCreator: Lazy<ChromeViewModel>
    @Inject
    lateinit var promptMessageViewModelCreator: Lazy<ShoppingSearchPromptViewModel>
    private lateinit var chromeViewModel: ChromeViewModel
    private lateinit var bottomBarViewModel: BottomBarViewModel
    private lateinit var bottomBarItemAdapter: BottomBarItemAdapter
    private lateinit var shoppingSearchPromptMessageViewModel: ShoppingSearchPromptViewModel
    private lateinit var shoppingSearchPromptMessageBehavior: BottomSheetBehavior<*>

    private var systemVisibility = ViewUtils.SYSTEM_UI_VISIBILITY_NONE
    private val downloadCallback = DownloadCallback()

    private lateinit var findInPage: FindInPage
    private lateinit var rootView: ThemedCoordinatorLayout
    private lateinit var shoppingSearchViewStub: ViewStub
    private lateinit var sessionManager: SessionManager
    private lateinit var appBarBgTransition: TransitionDrawable
    private lateinit var statusBarBgTransition: TransitionDrawable
    private var webContextMenu: Dialog? = null

    // GeoLocationPermission
    private var geolocationOrigin: String? = null
    private var geolocationCallback: GeolocationPermissions.Callback? = null
    private var geoDialog: AlertDialog? = null

    private var fullscreenCallback: FullscreenCallback? = null
    var isLoading = false
        private set

    // Set an initial WeakReference so we never have to handle loadStateListenerWeakReference being null
    // (i.e. so we can always just .get()).
    private var loadStateListenerWeakReference = WeakReference<LoadStateListener?>(null)

    @set:VisibleForTesting
    var captureStateListener: CaptureStateListener? = null

    // pending action for file-choosing
    private var fileChooseAction: FileChooseAction? = null
    private lateinit var permissionHandler: PermissionHandler
    private var hasPendingScreenCaptureTask = false
    private var pendingScreenCaptureTelemetryData: ScreenCaptureTelemetryData? = null
    private val sessionObserver = SessionObserver()
    private val managerObserver: SessionManager.Observer = SessionManagerObserver(sessionObserver)
    private var downloadIndicatorIntro: View? = null
    private var landscapeStartTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        this.appComponent().inject(this)
        super.onCreate(savedInstanceState)
        bottomBarViewModel = getActivityViewModel(bottomBarViewModelCreator)
        chromeViewModel = getActivityViewModel(chromeViewModelCreator)
        shoppingSearchPromptMessageViewModel = getActivityViewModel(promptMessageViewModelCreator)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState != null) {
            permissionHandler.onRestoreInstanceState(savedInstanceState)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        permissionHandler = PermissionHandler(object : PermissionHandle {
            override fun doActionDirect(permission: String, actionId: Int, params: Parcelable?) {
                when (actionId) {
                    ACTION_DOWNLOAD -> {
                        if (getContext() == null) {
                            Log.w(ScreenNavigator.BROWSER_FRAGMENT_TAG, "No context to use, abort callback onDownloadStart")
                            return
                        }
                        val download = params as Download
                        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                            // We do have the permission to write to the external storage. Proceed with the download.
                            queueDownload(download)
                        }
                    }
                    ACTION_PICK_FILE -> fileChooseAction?.startChooserActivity()
                    ACTION_GEO_LOCATION -> showGeolocationPermissionPrompt()
                    ACTION_CAPTURE -> showLoadingAndCapture(params as ScreenCaptureTelemetryData)
                    else -> throw IllegalArgumentException("Unknown actionId")
                }
            }

            private fun actionDownloadGranted(parcelable: Parcelable?) {
                queueDownload(parcelable as? Download)
            }

            private fun actionPickFileGranted() {
                fileChooseAction?.startChooserActivity()
            }

            private fun actionCaptureGranted(telemetryData: ScreenCaptureTelemetryData) {
                setPendingScreenCaptureTask(telemetryData)
            }

            private fun doActionGrantedOrSetting(actionId: Int, params: Parcelable?) {
                when (actionId) {
                    ACTION_DOWNLOAD -> actionDownloadGranted(params)
                    ACTION_PICK_FILE -> actionPickFileGranted()
                    ACTION_GEO_LOCATION -> showGeolocationPermissionPrompt()
                    ACTION_CAPTURE -> actionCaptureGranted(params as ScreenCaptureTelemetryData)
                    else -> throw IllegalArgumentException("Unknown actionId")
                }
            }

            override fun doActionGranted(permission: String, actionId: Int, params: Parcelable?) {
                doActionGrantedOrSetting(actionId, params)
            }

            override fun doActionSetting(permission: String, actionId: Int, params: Parcelable?) {
                doActionGrantedOrSetting(actionId, params)
            }

            override fun doActionNoPermission(permission: String, actionId: Int, params: Parcelable?) {
                when (actionId) {
                    ACTION_DOWNLOAD -> {
                    }
                    ACTION_PICK_FILE -> fileChooseAction?.let {
                        it.cancel()
                        fileChooseAction = null
                    }
                    ACTION_GEO_LOCATION -> if (geolocationCallback != null) {
                        // I'm not sure why it's so. This method already on Main thread.
                        // But if I don't do this, webview will keeps requesting for permission.
                        // See https://github.com/mozilla-tw/Rocket/blob/765f6a1ddbc2b9058813e930f63c62a9797c5fa0/app/src/webkit/java/org/mozilla/focus/webkit/FocusWebChromeClient.java#L126
                        ThreadUtils.postToMainThread { rejectGeoRequest(false) }
                    }
                    ACTION_CAPTURE -> {
                    }
                    else -> throw IllegalArgumentException("Unknown actionId")
                }
            }

            override fun makeAskAgainSnackBar(actionId: Int): Snackbar {
                return PermissionHandler.makeAskAgainSnackBar(this@BrowserFragment, requireActivity().findViewById(R.id.container), getAskAgainSnackBarString(actionId))
            }

            private fun getAskAgainSnackBarString(actionId: Int): Int {
                return if (actionId == ACTION_DOWNLOAD || actionId == ACTION_PICK_FILE || actionId == ACTION_CAPTURE) {
                    R.string.permission_toast_storage
                } else if (actionId == ACTION_GEO_LOCATION) {
                    R.string.permission_toast_location
                } else {
                    throw IllegalArgumentException("Unknown Action")
                }
            }

            private fun getPermissionDeniedToastString(actionId: Int): Int {
                return if (actionId == ACTION_DOWNLOAD || actionId == ACTION_PICK_FILE || actionId == ACTION_CAPTURE) {
                    R.string.permission_toast_storage_deny
                } else if (actionId == ACTION_GEO_LOCATION) {
                    R.string.permission_toast_location_deny
                } else {
                    throw IllegalArgumentException("Unknown Action")
                }
            }

            override fun requestPermissions(actionId: Int) {
                when (actionId) {
                    ACTION_DOWNLOAD, ACTION_CAPTURE -> this@BrowserFragment.requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), actionId)
                    ACTION_PICK_FILE -> this@BrowserFragment.requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), actionId)
                    ACTION_GEO_LOCATION -> this@BrowserFragment.requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), actionId)
                    else -> throw IllegalArgumentException("Unknown Action")
                }
            }

            override fun permissionDeniedToast(actionId: Int) {
                Toast.makeText(getContext(), getPermissionDeniedToastString(actionId), Toast.LENGTH_LONG).show()
            }
        })
    }

    override fun onPause() {
        sessionManager.pause()
        super.onPause()
    }

    override fun applyLocale() {
        // We create and destroy a new WebView here to force the internal state of WebView to know
        // about the new language. See issue #666.
        val unneeded = WebView(context)
        unneeded.destroy()
    }

    override fun onResume() {
        sessionManager.resume()
        super.onResume()
        if (hasPendingScreenCaptureTask) {
            showLoadingAndCapture(pendingScreenCaptureTelemetryData)
            clearPendingScreenCaptureTask()
        }
    }

    private fun updateURL(url: String?) {
        if (UrlUtils.isInternalErrorURL(url)) {
            return
        }
        display_url.text = UrlUtils.stripUserInfo(url)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_browser, container, false) as ThemedCoordinatorLayout
        shoppingSearchViewStub = rootView.shopping_search_stub
        return rootView
    }

    private fun observeShoppingSearchPromptMessageViewModel() {
        shoppingSearchPromptMessageViewModel.openShoppingSearch.observe(viewLifecycleOwner, Observer {
            startActivity(getStartIntent(requireContext()))
            ScreenNavigator.get(context).popToHomeScreen(false)
        })
        shoppingSearchPromptMessageViewModel.promptVisibilityState.observe(viewLifecycleOwner, Observer { visibilityState: VisibilityState? ->
            if (shoppingSearchViewStub.parent != null) {
                setupShoppingSearchPrompt(shoppingSearchViewStub.inflate())
            }
            if (visibilityState is Expanded) {
                changeShoppingSearchPromptMessageState(BottomSheetBehavior.STATE_EXPANDED)
            } else {
                changeShoppingSearchPromptMessageState(BottomSheetBehavior.STATE_HIDDEN)
            }
        })
        shoppingSearchPromptMessageViewModel.shoppingSiteList.observe(viewLifecycleOwner, Observer<List<ShoppingSiteItem?>> {
            shoppingSearchPromptMessageViewModel.checkShoppingSearchPromptVisibility(url)
        })
    }

    private fun setupShoppingSearchPrompt(view: View) {
        shoppingSearchPromptMessageBehavior = BottomSheetBehavior.from(view.findViewById<CoordinatorLayout>(R.id.bottom_sheet)).apply {
            setBottomSheetCallback(object : BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    when (newState) {
                        BottomSheetBehavior.STATE_EXPANDED -> shoppingSearchPromptMessageViewModel.onPromptIsShown()
                        BottomSheetBehavior.STATE_HIDDEN -> shoppingSearchPromptMessageViewModel.onPromptIsDismissed()
                        BottomSheetBehavior.STATE_DRAGGING -> shoppingSearchPromptMessageViewModel.onPromptIsDragged()
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    // Do nothing
                }
            })
        }
        view.findViewById<Button>(R.id.bottom_sheet_search).setOnClickListener {
            shoppingSearchPromptMessageViewModel.onShoppingSearchPromptButtonClicked()
        }
    }

    private fun changeShoppingSearchPromptMessageState(state: Int) {
        shoppingSearchPromptMessageBehavior.state = state
    }

    private fun observeChromeAction() {
        chromeViewModel.isTurboModeEnabled.observe(viewLifecycleOwner, Observer { enabled: Boolean ->
            setContentBlockingEnabled(enabled)
        })
        chromeViewModel.isBlockImageEnabled.observe(viewLifecycleOwner, Observer { enabled: Boolean ->
            setImageBlockingEnabled(enabled)
        })
        chromeViewModel.isBlockJavaScriptEnabled.observe(viewLifecycleOwner, Observer { enabled: Boolean ->
            setJavaScriptBlockingEnabled(enabled)
        })
        chromeViewModel.doScreenshot.observe(viewLifecycleOwner, Observer { telemetryData: ScreenCaptureTelemetryData? ->
            permissionHandler.tryAction(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, ACTION_CAPTURE, telemetryData)
        })
        chromeViewModel.refreshOrStop.observe(viewLifecycleOwner, Observer {
            if (isLoading) {
                stop()
            } else {
                reload()
            }
        })
        chromeViewModel.goNext.observe(viewLifecycleOwner, Observer {
            if (canGoForward()) {
                goForward()
            }
        })
        chromeViewModel.showFindInPage.observe(viewLifecycleOwner, Observer {
            if (chromeViewModel.navigationState.value?.isBrowser == true) {
                showFindInPage()
            }
        })
        chromeViewModel.currentUrl.observe(viewLifecycleOwner, Observer {
            appbar.setExpanded(true)
            browser_bottom_bar.slideUp()
        })
    }

    private fun observeNightMode() {
        chromeViewModel.isNightMode.observe(viewLifecycleOwner,
                Observer { (isEnabled) -> setNightModeEnabled(isEnabled) })
    }

    private fun setupBottomBar() {
        browser_bottom_bar.setOnItemClickListener(object : BottomBar.OnItemClickListener {
            override fun onItemClick(type: Int, position: Int) {
                when (type) {
                    BottomBarItemAdapter.TYPE_TAB_COUNTER -> {
                        chromeViewModel.showTabTray.call()
                        TelemetryWrapper.showTabTrayToolbar(Extra_Value.WEBVIEW, position, isInLandscape())
                    }
                    BottomBarItemAdapter.TYPE_MENU -> {
                        chromeViewModel.showMenu.call()
                        TelemetryWrapper.showMenuToolbar(Extra_Value.WEBVIEW, position)
                    }
                    BottomBarItemAdapter.TYPE_HOME -> {
                        chromeViewModel.showNewTab.call()
                        TelemetryWrapper.clickAddTabToolbar(Extra_Value.WEBVIEW, position, isInLandscape())
                    }
                    BottomBarItemAdapter.TYPE_SEARCH -> {
                        chromeViewModel.showUrlInput.value = url
                        TelemetryWrapper.clickToolbarSearch(Extra_Value.WEBVIEW, position, isInLandscape())
                    }
                    BottomBarItemAdapter.TYPE_CAPTURE -> chromeViewModel.onDoScreenshot(ScreenCaptureTelemetryData(Extra_Value.WEBVIEW, position))
                    BottomBarItemAdapter.TYPE_PIN_SHORTCUT -> {
                        chromeViewModel.pinShortcut.call()
                        TelemetryWrapper.clickAddToHome(Extra_Value.WEBVIEW, position)
                    }
                    BottomBarItemAdapter.TYPE_BOOKMARK -> {
                        val isActivated = bottomBarItemAdapter.getItem(BottomBarItemAdapter.TYPE_BOOKMARK)?.view?.isActivated == true
                        TelemetryWrapper.clickToolbarBookmark(!isActivated, Extra_Value.WEBVIEW, position)
                        chromeViewModel.toggleBookmark()
                    }
                    BottomBarItemAdapter.TYPE_REFRESH -> {
                        chromeViewModel.refreshOrStop.call()
                        TelemetryWrapper.clickToolbarReload(Extra_Value.WEBVIEW, position, isInLandscape())
                    }
                    BottomBarItemAdapter.TYPE_SHARE -> {
                        chromeViewModel.share.call()
                        TelemetryWrapper.clickToolbarShare(Extra_Value.WEBVIEW, position, isInLandscape())
                    }
                    BottomBarItemAdapter.TYPE_NEXT -> {
                        chromeViewModel.goNext.call()
                        TelemetryWrapper.clickToolbarForward(Extra_Value.WEBVIEW, position)
                    }
                    else -> throw IllegalArgumentException("Unhandled bottom bar item, type: $type")
                }
            }
        })
        browser_bottom_bar.setOnItemLongClickListener(object : BottomBar.OnItemLongClickListener {
            override fun onItemLongClick(type: Int, position: Int): Boolean {
                if (type == BottomBarItemAdapter.TYPE_MENU) {
                    // Long press menu always show download panel
                    chromeViewModel.showDownloadPanel.call()
                    longPressDownloadIndicator()
                    return true
                }
                return false
            }
        })
        bottomBarItemAdapter = BottomBarItemAdapter(browser_bottom_bar, BottomBarItemAdapter.Theme.Light)
        bottomBarViewModel.items.observe(viewLifecycleOwner, Observer { types: List<BottomBarItemAdapter.ItemData> ->
            bottomBarItemAdapter.setItems(types)
        })
        chromeViewModel.isNightMode.switchFrom(bottomBarViewModel.items)
                .observe(viewLifecycleOwner, Observer { (isEnabled) ->
                    bottomBarItemAdapter.setNightMode(isEnabled)
                })
        chromeViewModel.tabCount.switchFrom(bottomBarViewModel.items)
                .observe(viewLifecycleOwner, Observer { count: Int ->
                    bottomBarItemAdapter.setTabCount(count, true)
                })
        chromeViewModel.isRefreshing.switchFrom(bottomBarViewModel.items)
                .observe(viewLifecycleOwner, Observer { isRefreshing: Boolean ->
                    bottomBarItemAdapter.setRefreshing(isRefreshing)
                })
        chromeViewModel.canGoForward.switchFrom(bottomBarViewModel.items)
                .observe(viewLifecycleOwner, Observer { canGoForward: Boolean ->
                    bottomBarItemAdapter.setCanGoForward(canGoForward)
                })
        chromeViewModel.isCurrentUrlBookmarked.switchFrom(bottomBarViewModel.items)
                .observe(viewLifecycleOwner, Observer { isBookmark: Boolean ->
                    bottomBarItemAdapter.setBookmark(isBookmark)
                })
        setupDownloadIndicator()
    }

    private fun isInLandscape(): Boolean {
        return resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    private fun setupDownloadIndicator() {
        val downloadIndicatorViewModel = getActivityViewModel(downloadIndicatorViewModelCreator)
        downloadIndicatorViewModel.downloadIndicatorObservable.switchFrom(bottomBarViewModel.items)
                .observe(viewLifecycleOwner, Observer { status: DownloadIndicatorViewModel.Status ->
                    when (status) {
                        DownloadIndicatorViewModel.Status.DOWNLOADING -> bottomBarItemAdapter.setDownloadState(BottomBarItemAdapter.DOWNLOAD_STATE_DOWNLOADING)
                        DownloadIndicatorViewModel.Status.UNREAD -> bottomBarItemAdapter.setDownloadState(BottomBarItemAdapter.DOWNLOAD_STATE_UNREAD)
                        DownloadIndicatorViewModel.Status.WARNING -> bottomBarItemAdapter.setDownloadState(BottomBarItemAdapter.DOWNLOAD_STATE_WARNING)
                        DownloadIndicatorViewModel.Status.DEFAULT -> bottomBarItemAdapter.setDownloadState(BottomBarItemAdapter.DOWNLOAD_STATE_DEFAULT)
                    }
                    val eventHistory = Settings.getInstance(activity).eventHistory
                    if (!eventHistory.contains(Settings.Event.ShowDownloadIndicatorIntro) && status !== DownloadIndicatorViewModel.Status.DEFAULT) {
                        eventHistory.add(Settings.Event.ShowDownloadIndicatorIntro)
                        val menuItem = bottomBarItemAdapter.getItem(BottomBarItemAdapter.TYPE_MENU)
                        if (menuItem?.view != null) {
                            initDownloadIndicatorIntroView(this, menuItem.view, rootView, object : OnViewInflated {
                                override fun onInflated(view: View) {
                                    downloadIndicatorIntro = view
                                }
                            })
                        }
                    }
                })
    }

    override fun onViewCreated(container: View, savedInstanceState: Bundle?) {
        super.onViewCreated(container, savedInstanceState)

        appbar.setOnApplyWindowInsetsListener { v: View, insets: WindowInsets ->
            (v.layoutParams as MarginLayoutParams).topMargin = insets.systemWindowInsetTop
            inset_cover.layoutParams.height = insets.systemWindowInsetTop
            insets
        }
        main_content.setOnApplyWindowInsetsListener { v: View, insets: WindowInsets ->
            v.setPadding(0, 0, 0, insets.systemWindowInsetTop)
            insets
        }
        appBarBgTransition = urlbar.background as TransitionDrawable
        statusBarBgTransition = inset_cover.background as TransitionDrawable
        observeChromeAction()
        setupBottomBar()
        findInPage = FindInPage(container)
        initialiseNormalBrowserUi()
        sessionManager = TabsSessionProvider.getOrThrow(activity)
        sessionManager.register(managerObserver, this, false)
        observeShoppingSearchPromptMessageViewModel()
        observeNightMode()

        // restore WebView state
        if (savedInstanceState != null) {
            // Fragment was destroyed
            // FIXME: Obviously, only restore current tab is not enough
            val focusTab = sessionManager.focusSession
            if (focusTab != null) {
                val tabView = focusTab.engineSession?.tabView
                if (tabView != null) {
                    tabView.restoreViewState(savedInstanceState)
                } else {
                    // Focus to tab again to force initialization.
                    sessionManager.switchToTab(focusTab.id)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        permissionHandler.onActivityResult(activity, requestCode, resultCode, data)
        if (requestCode == FileChooseAction.REQUEST_CODE_CHOOSE_FILE) {
            val done = fileChooseAction == null || fileChooseAction?.onFileChose(resultCode, data) == true
            if (done) {
                fileChooseAction = null
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateBottomBarLayout()
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            bottomBarViewModel.onScreenRotatedToLandscape(true)
            onLandscapeModeStart()
        } else {
            bottomBarViewModel.onScreenRotatedToLandscape(false)
            onLandscapeModeFinish()
        }
        refreshVideoContainer()
    }

    private fun updateBottomBarLayout() {
        val bottomBarHeight: Int = browser_bottom_bar.resources.getDimensionPixelOffset(R.dimen.fixed_menu_height)
        browser_bottom_bar.layoutParams = browser_bottom_bar.layoutParams.apply {
            height = bottomBarHeight
        }
        browser_bottom_bar.onScreenRotated()
    }

    // Workaround for full-screen WebView issue that the video doesn't fit the viewport
    // after rotating the device from portrait to landscape and vice versa. It could reduce
    // the issue happened rate by changing the video view layout size to a slight smaller size
    // then add to the full screen size again when the device is rotated.
    private fun refreshVideoContainer() {
        if (video_container.visibility == View.VISIBLE) {
            updateVideoContainerWithLayoutParams(FrameLayout.LayoutParams(
                (video_container.height * 0.99).toInt(),
                (video_container.width * 0.99).toInt()
            ))
            video_container.post {
                if (video_container.visibility == View.VISIBLE) {
                    updateVideoContainerWithLayoutParams(FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    ))
                }
            }
        }
    }

    private fun updateVideoContainerWithLayoutParams(params: FrameLayout.LayoutParams) {
        val fullscreenContentView = video_container.getChildAt(0)
        if (fullscreenContentView != null) {
            video_container.removeAllViews()
            video_container.addView(fullscreenContentView, params)
        }
    }

    private fun onLandscapeModeStart() {
        landscapeStartTime = System.currentTimeMillis()
        TelemetryWrapper.enterLandscapeMode()
    }

    private fun onLandscapeModeFinish() {
        if (landscapeStartTime == 0L) {
            return
        }
        val duration = System.currentTimeMillis() - landscapeStartTime
        TelemetryWrapper.exitLandscapeMode(duration)
        landscapeStartTime = 0L
    }

    override fun goBackground() {
        val current = sessionManager.focusSession
        if (current != null) {
            val es = current.engineSession
            if (es != null) {
                es.detach()
                val tabView = es.tabView
                if (tabView != null) {
                    webview_slot.removeView(tabView.view)
                }
            }
        }
    }

    override fun goForeground() {
        val current = sessionManager.focusSession
        if (webview_slot.childCount == 0 && current != null) {
            val es = current.engineSession
            if (es != null) {
                val tabView = es.tabView
                if (tabView != null) {
                    webview_slot.addView(tabView.view)
                }
            }
        }
    }

    private fun initialiseNormalBrowserUi() {
        display_url.setOnClickListener {
            chromeViewModel.showUrlInput.value = url
            // TODO: Needs to confirm with bi that what vertical should be passed into in normal browser using cases
            // TODO: For now just pass a empty string
            TelemetryWrapper.clickUrlbar("", isInLandscape())
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        permissionHandler.onSaveInstanceState(outState)
        sessionManager.focusSession?.engineSession?.tabView?.saveViewState(outState)

        // Workaround for #1107 TransactionTooLargeException
        // since Android N, system throws a exception rather than just a warning(then drop bundle)
        // To set a threshold for dropping WebView state manually
        // refer: https://issuetracker.google.com/issues/37103380
        val key = "WEBVIEW_CHROMIUM_STATE"
        if (outState.containsKey(key)) {
            val size = outState.getByteArray(key)?.size ?: -1
            if (size > BUNDLE_MAX_SIZE) {
                outState.remove(key)
            }
        }
        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        if (systemVisibility != ViewUtils.SYSTEM_UI_VISIBILITY_NONE) {
            sessionManager.focusSession?.engineSession?.tabView?.performExitFullScreen()
        }
        dismissGeoDialog()
        super.onStop()
    }

    override fun onDestroyView() {
        sessionManager.unregister(managerObserver)
        super.onDestroyView()
    }

    fun setContentBlockingEnabled(enabled: Boolean) {
        // TODO: Better if we can move this logic to some setting-like classes, and provider interface
        // for configuring blocking function of each tab.
        for (session in sessionManager.getTabs()) {
            session.engineSession?.tabView?.setContentBlockingEnabled(enabled)
        }
    }

    fun setImageBlockingEnabled(enabled: Boolean) {
        // TODO: Better if we can move this logic to some setting-like classes, and provider interface
        // for configuring blocking function of each tab.
        for (session in sessionManager.getTabs()) {
            session.engineSession?.tabView?.setImageBlockingEnabled(enabled)
        }
    }

    private fun setJavaScriptBlockingEnabled(enabled: Boolean) {
        // TODO: Better if we can move this logic to some setting-like classes, and provider interface
        // for configuring JavaScript blocking function of each tab.
        for (session in sessionManager.getTabs()) {
            session.engineSession?.tabView?.setJavaScriptBlockingEnabled(enabled)
        }
    }

    interface LoadStateListener {
        fun isLoadingChanged(isLoading: Boolean)
    }

    @VisibleForTesting
            /**
             * Set a (singular) LoadStateListener. Only one listener is supported at any given time. Setting
             * a new listener means any previously set listeners will be dropped. This is only intended
             * to be used by NavigationItemViewHolder. If you want to use this method for any other
             * parts of the codebase, please extend it to handle a list of listeners. (We would also need
             * to automatically clean up expired listeners from that list, probably when adding to that list.)
             *
             * @param listener The listener to notify of load state changes. Only a weak reference will be kept,
             * no more calls will be sent once the listener is garbage collected.
             */
    fun setIsLoadingListener(listener: LoadStateListener?) {
        loadStateListenerWeakReference = WeakReference(listener)
    }

    private fun setPendingScreenCaptureTask(telemetryData: ScreenCaptureTelemetryData) {
        hasPendingScreenCaptureTask = true
        pendingScreenCaptureTelemetryData = telemetryData
    }

    private fun clearPendingScreenCaptureTask() {
        hasPendingScreenCaptureTask = false
        pendingScreenCaptureTelemetryData = null
    }

    private fun showLoadingAndCapture(telemetryData: ScreenCaptureTelemetryData?) {
        if (!isResumed) {
            return
        }
        clearPendingScreenCaptureTask()
        val capturingFragment = ScreenCaptureDialogFragment.newInstance()
        val portraitState = portraitStateModel
        if (portraitState != null) {
            portraitState.request(PortraitComponent.ScreenCapture)
            capturingFragment.addOnDismissListener {
                portraitState.cancelRequest(PortraitComponent.ScreenCapture)
            }
        }
        capturingFragment.show(childFragmentManager, "capturingFragment")
        // Post delay to wait for Dialog to show
        Handler().postDelayed(CaptureRunnable(context, this, capturingFragment, requireActivity().findViewById(R.id.container), telemetryData), CAPTURE_WAIT_INTERVAL.toLong())
    }

    private fun updateIsLoading(isLoading: Boolean) {
        this.isLoading = isLoading
        val currentListener = loadStateListenerWeakReference.get()
        currentListener?.isLoadingChanged(isLoading)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        permissionHandler.onRequestPermissionsResult(context, requestCode, permissions, grantResults)
    }

    /**
     * Use Android's Download Manager to queue this download.
     */
    private fun queueDownload(download: Download?) {
        val activity: Activity? = activity
        if (activity == null || download == null) {
            return
        }
        EnqueueDownloadTask(requireActivity(), download, url).execute()
    }

    /*
     * show webview geolocation permission prompt
     */
    private fun showGeolocationPermissionPrompt() {
        if (!isPopupWindowAllowed) {
            return
        }
        if (geolocationCallback == null) {
            return
        }
        if (geoDialog?.isShowing == true) {
            return
        }
        val allowed = GeoPermissionCache.getAllowed(geolocationOrigin)
        if (allowed != null) {
            geolocationCallback?.invoke(geolocationOrigin, allowed, false)
        } else {
            geoDialog = buildGeoPromptDialog().also {
                it.show()
            }
        }
    }

    fun dismissGeoDialog() {
        geoDialog?.let {
            it.dismiss()
            geoDialog = null
        }
    }

    @VisibleForTesting
    fun buildGeoPromptDialog(): AlertDialog {
        val customContent = LayoutInflater.from(context).inflate(R.layout.dialog_permission_request, null)
        val checkBox = customContent.findViewById<CheckedTextView>(R.id.cache_my_decision)
        checkBox.text = getString(R.string.geolocation_dialog_message_cache_it, getString(R.string.app_name))
        checkBox.setOnClickListener { checkBox.toggle() }
        val builder = AlertDialog.Builder(requireContext())
        builder.setView(customContent)
                .setMessage(getString(R.string.geolocation_dialog_message, geolocationOrigin))
                .setCancelable(true)
                .setPositiveButton(getString(R.string.geolocation_dialog_allow)) { _, _ -> acceptGeoRequest(checkBox.isChecked) }
                .setNegativeButton(getString(R.string.geolocation_dialog_block)) { _, _ -> rejectGeoRequest(checkBox.isChecked) }
                .setOnDismissListener { rejectGeoRequest(false) }
                .setOnCancelListener { rejectGeoRequest(false) }
        return builder.create()
    }

    private fun acceptGeoRequest(cacheIt: Boolean) {
        if (geolocationCallback == null) {
            return
        }
        if (cacheIt) {
            GeoPermissionCache.putAllowed(geolocationOrigin, java.lang.Boolean.TRUE)
        }
        geolocationCallback?.invoke(geolocationOrigin, true, false)
        geolocationOrigin = ""
        geolocationCallback = null
    }

    private fun rejectGeoRequest(cacheIt: Boolean) {
        if (geolocationCallback == null) {
            return
        }
        if (cacheIt) {
            GeoPermissionCache.putAllowed(geolocationOrigin, java.lang.Boolean.FALSE)
        }
        geolocationCallback?.invoke(geolocationOrigin, false, false)
        geolocationOrigin = ""
        geolocationCallback = null
    }

    // No SafeIntent needed here because intent.getAction() is safe (SafeIntent simply calls intent.getAction()
    // without any wrapping):
    private val isStartedFromExternalApp: Boolean
        get() {
            val activity = activity ?: return false

            // No SafeIntent needed here because intent.getAction() is safe (SafeIntent simply calls intent.getAction()
            // without any wrapping):
            val intent = activity.intent
            val isFromInternal = intent != null && intent.getBooleanExtra(IntentUtils.EXTRA_IS_INTERNAL_REQUEST, false)
            return intent != null && Intent.ACTION_VIEW == intent.action && !isFromInternal
        }

    override fun onBackPressed(): Boolean {
        if (findInPage.onBackPressed()) {
            return true
        }

        // After we apply the full screen rotation workaround - 'refreshVideoContainer',
        // it may not be able to get 'onExitFullScreen' callback from WebChromeClient. Just call it here
        // to leave the full screen mode.
        if (video_container.visibility == View.VISIBLE) {
            sessionObserver.onExitFullScreen()
            return true
        }
        if (canGoBack()) {
            // Go back in web history
            goBack()
        } else {
            val focus = sessionManager.focusSession
            if (focus == null) {
                return false
            } else if (focus.isFromExternal || focus.hasParentTab()) {
                sessionManager.closeTab(focus.id)
            } else {
                ScreenNavigator.get(context).popToHomeScreen(true)
            }
        }
        return true
    }

    /**
     * @param url target url
     * @param openNewTab whether to load url in a new tab or not
     * @param isFromExternal if this url is started from external VIEW intent
     * @param onViewReadyCallback callback to notify that web view is ready for showing.
     */
    override fun loadUrl(
        url: String,
        openNewTab: Boolean,
        isFromExternal: Boolean,
        onViewReadyCallback: Runnable?
    ) {
        updateURL(url)
        if (SupportUtils.isUrl(url)) {
            if (openNewTab) {
                sessionManager.addTab(url, TabUtil.argument(null, isFromExternal, true))
                // Per spec, if download indicator intro view is showed when new tabb is opened, just dismiss it anyway.
                dismissDownloadIndicatorIntroView()
                // In case we call SessionManager#addTab(), which is an async operation calls back in the next
                // message loop. By posting this runnable we can call back in the same message loop with
                // TabsContentListener#onFocusChanged(), which is when the view is ready and being attached.
                ThreadUtils.postToMainThread(onViewReadyCallback)
            } else {
                val currentTab = sessionManager.focusSession
                if (currentTab?.engineSession?.tabView != null) {
                    currentTab.engineSession?.tabView?.loadUrl(url)
                    onViewReadyCallback?.run()
                } else {
                    sessionManager.addTab(url, TabUtil.argument(null, isFromExternal, true))
                    ThreadUtils.postToMainThread(onViewReadyCallback)
                }
            }
        } else if (AppConstants.isDevBuild()) {
            // throw exception to highlight this issue, except release build.
            throw RuntimeException("trying to open a invalid url: $url")
        }
    }

    override fun switchToTab(tabId: String) {
        if (!TextUtils.isEmpty(tabId)) {
            sessionManager.switchToTab(tabId)
        }
    }

    // getUrl() is used for things like sharing the current URL. We could try to use the webview,
    // but sometimes it's null, and sometimes it returns a null URL. Sometimes it returns a data:
    // URL for error pages. The URL we show in the toolbar is (A) always correct and (B) what the
    // user is probably expecting to share, so lets use that here:
    val url: String
        get() = display_url.text.toString()

    private fun canGoForward(): Boolean = sessionManager.focusSession?.canGoForward == true

    private fun canGoBack(): Boolean = sessionManager.focusSession?.canGoBack == true

    private fun goBack() {
        val currentTab = sessionManager.focusSession
        if (currentTab != null) {
            val current = currentTab.engineSession?.tabView
            // The Session.canGoBack property is mainly for UI display purpose and is only sampled
            // at onNavigationStateChange which is called at onPageFinished, onPageStarted and
            // onReceivedTitle. We do some sanity check here.
            if (current == null || !current.canGoBack()) {
                return
            }
            val webBackForwardList = (current as WebView).copyBackForwardList()
            val item = webBackForwardList.getItemAtIndex(webBackForwardList.currentIndex - 1)
            updateURL(item.url)
            current.goBack()
        }
    }

    private fun goForward() {
        val currentTab = sessionManager.focusSession
        if (currentTab != null) {
            val current = currentTab.engineSession?.tabView ?: return
            val webBackForwardList = (current as WebView).copyBackForwardList()
            val item = webBackForwardList.getItemAtIndex(webBackForwardList.currentIndex + 1)
            updateURL(item.url)
            current.goForward()
        }
    }

    private fun reload() {
        sessionManager.focusSession?.engineSession?.tabView?.reload()
    }

    private fun stop() {
        sessionManager.focusSession?.engineSession?.tabView?.stopLoading()
    }

    interface ScreenshotCallback {
        fun onCaptureComplete(title: String?, url: String?, bitmap: Bitmap?)
    }

    fun capturePage(callback: ScreenshotCallback): Boolean {
        val currentTab = sessionManager.focusSession ?: return false
        // Failed to get WebView
        val current = currentTab.engineSession?.tabView
        if (current == null || current !is WebView) {
            return false
        }
        val webView = current as WebView
        val content = getPageBitmap(webView) ?: return false
        // Failed to capture
        callback.onCaptureComplete(current.getTitle(), current.getUrl(), content)
        return true
    }

    fun dismissWebContextMenu() {
        webContextMenu?.let {
            it.dismiss()
            webContextMenu = null
        }
    }

    private fun getPageBitmap(webView: WebView): Bitmap? {
        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        return try {
            val bitmap = Bitmap.createBitmap(webView.width, (webView.contentHeight * displayMetrics.density).toInt(), Bitmap.Config.RGB_565)
            val canvas = Canvas(bitmap)
            webView.draw(canvas)
            bitmap
            // OOM may occur, even if OOMError is not thrown, operations during Bitmap creation may
            // throw other Exceptions such as NPE when the bitmap is very large.
        } catch (ex: Exception) {
            null
        } catch (ex: OutOfMemoryError) {
            null
        }
    }

    private val isPopupWindowAllowed: Boolean
        get() = ScreenNavigator.get(context).isBrowserInForeground &&
                isAdded && !TabTray.isShowing(parentFragmentManager)

    private fun showFindInPage() {
        val focusTab = sessionManager.focusSession
        if (focusTab != null) {
            appbar.setExpanded(false)
            browser_bottom_bar.visibility = View.INVISIBLE
            shoppingSearchViewStub.visibility = View.INVISIBLE
            rootView.isActivated = false
            findInPage.onDismissListener = {
                rootView.isActivated = true
                appbar.setExpanded(true)
                browser_bottom_bar.visibility = View.VISIBLE
                shoppingSearchViewStub.visibility = View.VISIBLE
            }
            findInPage.show(focusTab)
            TelemetryWrapper.findInPage(TelemetryWrapper.FIND_IN_PAGE.OPEN_BY_MENU)
        }
    }

    private val portraitStateModel: PortraitStateModel?
        get() {
            val activity = activity ?: return null
            return if (activity is MainActivity) {
                activity.portraitStateModel
            } else {
                if (BuildConfig.DEBUG) {
                    throw IllegalStateException("Only MainActivity has portrait state model")
                } else {
                    null
                }
            }
        }

    private fun hideFindInPage() {
        findInPage.hide()
    }

    private inner class SessionObserver : Session.Observer, TabViewEngineSession.Client {
        private var session: Session? = null
        private val historyInserter = HistoryInserter()

        // Some url may report progress from 0 again for the same url. filter them out to avoid
        // progress bar regression when scrolling.
        private var loadedUrl: String? = null
        override fun onLoadingStateChanged(session: Session, loading: Boolean) {
            isLoading = loading
            if (loading) {
                historyInserter.onTabStarted(session)
            } else {
                historyInserter.onTabFinished(session)
            }
            if (!isForegroundSession(session)) {
                return
            }
            if (loading) {
                loadedUrl = null
                chromeViewModel.onPageLoadingStarted()
                updateIsLoading(true)
                updateURL(session.url)
                appBarBgTransition.resetTransition()
                statusBarBgTransition.resetTransition()
            } else {
                // The URL which is supplied in onTabFinished() could be fake (see #301), but webview's
                // URL is always correct _except_ for error pages
                updateUrlFromWebView(session)
                chromeViewModel.onPageLoadingStopped()
                updateIsLoading(false)
                appBarBgTransition.startTransition(ANIMATION_DURATION)
                statusBarBgTransition.startTransition(ANIMATION_DURATION)
            }
        }

        override fun onSecurityChanged(session: Session, isSecure: Boolean) {
            site_identity.setImageLevel(if (isSecure) SITE_LOCK else SITE_GLOBE)
        }

        override fun onUrlChanged(session: Session, url: String?) {
            chromeViewModel.onFocusedUrlChanged(url)
            if (!isForegroundSession(session)) {
                return
            }
            updateURL(url)
            shoppingSearchPromptMessageViewModel.checkShoppingSearchPromptVisibility(url)
        }

        override fun handleExternalUrl(url: String?): Boolean {
            if (context == null) {
                Log.w(ScreenNavigator.BROWSER_FRAGMENT_TAG, "No context to use, abort callback handleExternalUrl")
                return false
            }
            val navigationState = chromeViewModel.navigationState.value
            if (navigationState != null && navigationState.isHome) {
                Log.w(ScreenNavigator.BROWSER_FRAGMENT_TAG, "Ignore external url when browser page is not on the front")
                return false
            }
            return IntentUtils.handleExternalUri(context, url)
        }

        override fun updateFailingUrl(url: String?, updateFromError: Boolean) {
            session?.let {
                historyInserter.updateFailingUrl(it, url, updateFromError)
            }
        }

        override fun onProgress(session: Session, progress: Int) {
            if (!isForegroundSession(session)) {
                return
            }
            hideFindInPage()
            if (sessionManager.focusSession != null) {
                val currentUrl = sessionManager.focusSession?.url
                val progressIsForLoadedUrl = TextUtils.equals(currentUrl, loadedUrl)
                // Some new url may give 100 directly and then start from 0 again. don't treat
                // as loaded for these urls;
                val urlBarLoadingToFinished = progress_bar.max != progress_bar.progress && progress == progress_bar.max
                if (urlBarLoadingToFinished) {
                    loadedUrl = currentUrl
                }
                if (progressIsForLoadedUrl) {
                    return
                }
            }
            progress_bar.progress = progress
        }

        override fun onShowFileChooser(
            es: TabViewEngineSession,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            if (!isForegroundSession(session)) {
                return false
            }
            TelemetryWrapper.browseFilePermissionEvent()
            return try {
                requireNotNull(filePathCallback)
                requireNotNull(fileChooserParams)
                fileChooseAction = FileChooseAction(this@BrowserFragment, filePathCallback, fileChooserParams)
                permissionHandler.tryAction(this@BrowserFragment, Manifest.permission.READ_EXTERNAL_STORAGE, ACTION_PICK_FILE, null)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

        override fun onTitleChanged(session: Session, title: String?) {
            chromeViewModel.onFocusedTitleChanged(title)
            if (!isForegroundSession(session)) {
                return
            }
            if (url != session.url) {
                updateURL(session.url)
            }
        }

        override fun onReceivedIcon(icon: Bitmap?) {}
        override fun onLongPress(session: Session, hitTarget: HitTarget) {
            if (activity == null) {
                Log.w(ScreenNavigator.BROWSER_FRAGMENT_TAG, "No context to use, abort callback onLongPress")
                return
            }
            webContextMenu = WebContextMenu.show(false, requireActivity(), downloadCallback, hitTarget)
        }

        override fun onEnterFullScreen(callback: FullscreenCallback, view: View?) {
            if (session == null) {
                return
            }
            if (!isForegroundSession(session)) {
                callback.fullScreenExited()
                return
            }
            fullscreenCallback = callback
            if (session?.engineSession?.tabView != null && view != null) {
                // Hide browser UI and web content
                appbar.visibility = View.INVISIBLE
                webview_container.visibility = View.INVISIBLE
                shoppingSearchViewStub.visibility = View.INVISIBLE
                browser_bottom_bar.visibility = View.INVISIBLE

                // Add view to video container and make it visible
                val params = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                video_container.addView(view, params)
                video_container.visibility = View.VISIBLE

                // Switch to immersive mode: Hide system bars other UI controls
                systemVisibility = ViewUtils.switchToImmersiveMode(activity)
            }
        }

        override fun onExitFullScreen() {
            if (session == null) {
                return
            }
            // Remove custom video views and hide container
            video_container.removeAllViews()
            video_container.visibility = View.GONE

            // Show browser UI and web content again
            appbar.visibility = View.VISIBLE
            webview_container.visibility = View.VISIBLE
            shoppingSearchViewStub.visibility = View.VISIBLE
            browser_bottom_bar.visibility = View.VISIBLE
            if (systemVisibility != ViewUtils.SYSTEM_UI_VISIBILITY_NONE) {
                ViewUtils.exitImmersiveMode(systemVisibility, activity)
            }

            // Notify renderer that we left fullscreen mode.
            fullscreenCallback?.let {
                it.fullScreenExited()
                fullscreenCallback = null
            }

            // WebView gets focus, but unable to open the keyboard after exit Fullscreen for Android 7.0+
            // We guess some component in WebView might lock focus
            // So when user touches the input text box on Webview, it will not trigger to open the keyboard
            // It may be a WebView bug.
            // The workaround is clearing WebView focus
            // The WebView will be normal when it gets focus again.
            // If android change behavior after, can remove this.
            session?.engineSession?.tabView?.let {
                if (it is WebView) {
                    it.clearFocus()
                }
            }
        }

        override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback?) {
            if (session == null) {
                return
            }
            if (!isForegroundSession(session) || !isPopupWindowAllowed) {
                return
            }
            geolocationOrigin = origin
            geolocationCallback = callback
            permissionHandler.tryAction(this@BrowserFragment,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    ACTION_GEO_LOCATION,
                    null)
        }

        fun changeSession(nextSession: Session?) {
            session?.unregister(this)
            session = nextSession?.also {
                it.register(this)
            }
        }

        private fun updateUrlFromWebView(source: Session) {
            if (sessionManager.focusSession != null) {
                val viewURL = sessionManager.focusSession?.url
                onUrlChanged(source, viewURL)
            }
        }

        private fun isForegroundSession(tab: Session?): Boolean {
            return sessionManager.focusSession == tab
        }

        override fun onFindResult(session: Session, result: FindResult) {
            findInPage.onFindResultReceived(result)
        }

        override fun onDownload(session: Session, download: mozilla.components.browser.session.Download): Boolean {
            val activity = activity
            if (activity == null || !activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                return false
            }
            val d = Download(download.url,
                    download.fileName,
                    download.userAgent,
                    "",
                    download.contentType,
                    requireNotNull(download.contentLength),
                    false)
            permissionHandler.tryAction(this@BrowserFragment, Manifest.permission.WRITE_EXTERNAL_STORAGE, ACTION_DOWNLOAD, d)
            return true
        }

        override fun onNavigationStateChanged(session: Session, canGoBack: Boolean, canGoForward: Boolean) {
            chromeViewModel.onNavigationStateChanged(canGoBack, canGoForward)
        }

        override fun onHttpAuthRequest(callback: HttpAuthCallback, host: String?, realm: String?) {
            val builder = HttpAuthenticationDialogBuilder.Builder(activity, host, realm)
                    .setOkListener { _: String?, _: String?, username: String?, password: String? ->
                        callback.proceed(username, password)
                    }
                    .setCancelListener { callback.cancel() }
                    .build()
            builder.createDialog()
            builder.show()
        }
    }

    private inner class SessionManagerObserver(private val sessionObserver: SessionObserver) : SessionManager.Observer {
        private var tabTransitionAnimator: ValueAnimator? = null

        override fun onFocusChanged(session: Session?, factor: Factor) {
            chromeViewModel.onFocusedUrlChanged(session?.url)
            chromeViewModel.onFocusedTitleChanged(session?.title)
            if (session == null) {
                if (factor === Factor.FACTOR_NO_FOCUS && !isStartedFromExternalApp) {
                    ScreenNavigator.get(context).popToHomeScreen(true)
                } else {
                    requireActivity().finish()
                }
            } else {
                transitToTab(session)
                refreshChrome(session)
            }
        }

        override fun onSessionAdded(session: Session, arguments: Bundle?) {
            if (arguments == null) {
                return
            }
            when (arguments.getInt(EXTRA_NEW_TAB_SRC, -1)) {
                SRC_CONTEXT_MENU -> onTabAddedByContextMenu(session, arguments)
                else -> {
                }
            }
        }

        override fun onSessionCountChanged(count: Int) {
            chromeViewModel.onTabCountChanged(count)
        }

        private fun transitToTab(targetTab: Session) {
            val tabView = targetTab.engineSession?.tabView
                    ?: throw RuntimeException("Tabview should be created at this moment and never be null")
            // ensure it does not have attach to parent earlier.
            targetTab.engineSession?.detach()
            val outView = findExistingTabView(webview_slot)
            webview_slot.removeView(outView)
            val inView = tabView.view
            webview_slot.addView(inView)
            this.sessionObserver.changeSession(targetTab)
            startTransitionAnimation(null, inView, null)
        }

        private fun refreshChrome(tab: Session) {
            geolocationOrigin = ""
            geolocationCallback = null
            dismissGeoDialog()
            updateURL(tab.url)
            shoppingSearchPromptMessageViewModel.checkShoppingSearchPromptVisibility(tab.url)
            progress_bar.progress = 0
            val identity = if (tab.securityInfo.secure) SITE_LOCK else SITE_GLOBE
            site_identity.setImageLevel(identity)
            hideFindInPage()
            val current = sessionManager.focusSession
            if (current != null) {
                chromeViewModel.onNavigationStateChanged(canGoBack(), canGoForward())
            }
            // check if newer config exists whenever navigating to Browser screen
            bottomBarViewModel.refresh()
        }

        private fun startTransitionAnimation(
            outView: View?,
            inView: View,
            finishCallback: Runnable?
        ) {
            stopTabTransition()
            inView.alpha = 0f
            if (outView != null) {
                outView.alpha = 1f
            }
            val duration = inView.resources.getInteger(R.integer.tab_transition_time)
            tabTransitionAnimator = ValueAnimator.ofFloat(0f, 1f).setDuration(duration.toLong()).apply {
                addUpdateListener { animation ->
                    val alpha = animation.animatedValue as Float
                    if (outView != null) {
                        outView.alpha = 1 - alpha
                    }
                    inView.alpha = alpha
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        finishCallback?.run()
                        inView.alpha = 1f
                        if (outView != null) {
                            outView.alpha = 1f
                        }
                    }
                })
            }.also {
                it.start()
            }
        }

        private fun findExistingTabView(parent: ViewGroup): View? {
            val viewCount = parent.childCount
            for (childIdx in 0 until viewCount) {
                val childView = parent.getChildAt(childIdx)
                if (childView is TabView) {
                    return (childView as TabView).view
                }
            }
            return null
        }

        private fun stopTabTransition() {
            tabTransitionAnimator?.run {
                if (isRunning) {
                    end()
                }
            }
        }

        private fun onTabAddedByContextMenu(tab: Session, arguments: Bundle) {
            if (!TabUtil.toFocus(arguments)) {
                Snackbar.make(rootView, R.string.new_background_tab_hint, Snackbar.LENGTH_LONG)
                        .setAction(R.string.new_background_tab_switch) { sessionManager.switchToTab(tab.id) }.show()
            }
        }

        override fun updateFailingUrl(url: String?, updateFromError: Boolean) {
            sessionObserver.updateFailingUrl(url, updateFromError)
        }

        override fun handleExternalUrl(url: String?): Boolean {
            return sessionObserver.handleExternalUrl(url)
        }

        override fun onShowFileChooser(es: TabViewEngineSession, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?): Boolean {
            return sessionObserver.onShowFileChooser(es, filePathCallback, fileChooserParams)
        }

        override fun onHttpAuthRequest(callback: HttpAuthCallback, host: String?, realm: String?) {
            sessionObserver.onHttpAuthRequest(callback, host, realm)
        }
    }

    internal inner class DownloadCallback : org.mozilla.rocket.tabs.web.DownloadCallback {
        override fun onDownloadStart(download: Download) {
            val activity = activity
            if (activity == null || !activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                return
            }
            permissionHandler.tryAction(this@BrowserFragment, Manifest.permission.WRITE_EXTERNAL_STORAGE, ACTION_DOWNLOAD, download)
        }
    }

    /**
     * TODO: This class records some intermediate data of each tab to avoid inserting duplicate
     * history, maybe it'd be better to make these data as per-tab data
     */
    private inner class HistoryInserter {
        private val failingUrls = WeakHashMap<Session, String?>()

        // Some url may have two onPageFinished for the same url. filter them out to avoid
        // adding twice to the history.
        private val lastInsertedUrls = WeakHashMap<Session, String>()
        fun onTabStarted(tab: Session) {
            lastInsertedUrls.remove(tab)
        }

        fun onTabFinished(tab: Session) {
            insertBrowsingHistory(tab)
        }

        fun updateFailingUrl(tab: Session, url: String?, updateFromError: Boolean) {
            val failingUrl = failingUrls[tab]
            if (!updateFromError && url != failingUrl) {
                failingUrls.remove(tab)
            } else {
                failingUrls[tab] = url
            }
        }

        private fun insertBrowsingHistory(tab: Session) {
            val urlToBeInserted = url
            val lastInsertedUrl = getLastInsertedUrl(tab)
            if (TextUtils.isEmpty(urlToBeInserted)) {
                return
            }
            if (urlToBeInserted == getFailingUrl(tab)) {
                return
            }
            if (urlToBeInserted == lastInsertedUrl) {
                return
            }
            tab.engineSession?.tabView?.insertBrowsingHistory()
            lastInsertedUrls[tab] = urlToBeInserted
        }

        private fun getFailingUrl(tab: Session): String {
            val url = failingUrls[tab]
            return requireNotNull(if (TextUtils.isEmpty(url)) "" else url)
        }

        private fun getLastInsertedUrl(tab: Session): String {
            val url = lastInsertedUrls[tab]
            return requireNotNull(if (TextUtils.isEmpty(url)) "" else url)
        }
    }

    fun checkToShowMyShotOnBoarding() {
        chromeViewModel.checkToShowMyShotOnBoarding()
    }

    override fun getFragment(): Fragment {
        return this
    }

    private fun setNightModeEnabled(enable: Boolean) {
        rootView.setNightMode(enable)
        browser_bottom_bar.setNightMode(enable)
        inset_cover.setNightMode(enable)
        toolbar_root.setNightMode(enable)
        display_url.setNightMode(enable)
        site_identity.setNightMode(enable)
        urlbar.setNightMode(enable)
        url_bar_divider.setNightMode(enable)
        ViewUtils.updateStatusBarStyle(!enable, requireActivity().window)
    }

    private fun dismissDownloadIndicatorIntroView() {
        downloadIndicatorIntro?.visibility = View.GONE
    }

    companion object {
        /**
         * Custom data that is passed when calling [SessionManager.addTab]
         */
        const val EXTRA_NEW_TAB_SRC = "extra_bkg_tab_src"
        const val SRC_CONTEXT_MENU = 0

        private const val ANIMATION_DURATION = 300
        private const val SITE_GLOBE = 0
        private const val SITE_LOCK = 1
        private const val BUNDLE_MAX_SIZE = 300 * 1000 // 300K
        private const val ACTION_DOWNLOAD = 0
        private const val ACTION_PICK_FILE = 1
        private const val ACTION_GEO_LOCATION = 2
        private const val ACTION_CAPTURE = 3
        private const val CAPTURE_WAIT_INTERVAL = 150
    }
}
