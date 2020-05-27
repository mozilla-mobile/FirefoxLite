package org.mozilla.rocket.privately.browse

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import dagger.Lazy
import kotlinx.android.synthetic.main.fragment_private_browser.browser_bottom_bar
import org.mozilla.focus.BuildConfig
import org.mozilla.focus.FocusApplication
import org.mozilla.focus.R
import org.mozilla.focus.download.EnqueueDownloadTask
import org.mozilla.focus.locale.LocaleAwareFragment
import org.mozilla.focus.menu.WebContextMenu
import org.mozilla.focus.navigation.ScreenNavigator
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.IntentUtils
import org.mozilla.focus.utils.ViewUtils
import org.mozilla.focus.web.BrowsingSession
import org.mozilla.focus.web.HttpAuthenticationDialogBuilder
import org.mozilla.focus.widget.AnimatedProgressBar
import org.mozilla.focus.widget.BackKeyHandleable
import org.mozilla.permissionhandler.PermissionHandle
import org.mozilla.permissionhandler.PermissionHandler
import org.mozilla.rocket.chrome.BottomBarItemAdapter
import org.mozilla.rocket.chrome.ChromeViewModel
import org.mozilla.rocket.chrome.PrivateBottomBarViewModel
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.getActivityViewModel
import org.mozilla.rocket.content.view.BottomBar
import org.mozilla.rocket.extension.nonNullObserve
import org.mozilla.rocket.extension.switchFrom
import org.mozilla.rocket.tabs.Session
import org.mozilla.rocket.tabs.SessionManager
import org.mozilla.rocket.tabs.TabView.FullscreenCallback
import org.mozilla.rocket.tabs.TabView.HitTarget
import org.mozilla.rocket.tabs.TabViewClient
import org.mozilla.rocket.tabs.TabViewEngineSession
import org.mozilla.rocket.tabs.TabsSessionProvider
import org.mozilla.rocket.tabs.utils.TabUtil
import org.mozilla.rocket.tabs.web.Download
import org.mozilla.rocket.tabs.web.DownloadCallback
import org.mozilla.threadutils.ThreadUtils
import org.mozilla.urlutils.UrlUtils
import javax.inject.Inject

private const val SITE_GLOBE = 0
private const val SITE_LOCK = 1
private const val ACTION_DOWNLOAD = 0

// TODO: remove after AC browser engine is stable
class BrowserFragmentLegacy : LocaleAwareFragment(),
        ScreenNavigator.BrowserScreen,
        BackKeyHandleable {

    @Inject
    lateinit var privateBottomBarViewModelCreator: Lazy<PrivateBottomBarViewModel>
    @Inject
    lateinit var chromeViewModelCreator: Lazy<ChromeViewModel>

    private lateinit var permissionHandler: PermissionHandler
    private lateinit var sessionManager: SessionManager
    private lateinit var observer: Observer
    private lateinit var bottomBarItemAdapter: BottomBarItemAdapter
    private lateinit var chromeViewModel: ChromeViewModel

    private lateinit var browserContainer: ViewGroup
    private lateinit var videoContainer: ViewGroup
    private lateinit var tabViewSlot: ViewGroup
    private lateinit var displayUrlView: TextView
    private lateinit var progressView: AnimatedProgressBar
    private lateinit var siteIdentity: ImageView

    private lateinit var toolbarRoot: ViewGroup

    private lateinit var trackerPopup: TrackerPopup

    private var systemVisibility = ViewUtils.SYSTEM_UI_VISIBILITY_NONE

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        chromeViewModel = getActivityViewModel(chromeViewModelCreator)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_private_browser_legacy, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val fragmentActivity = activity
        if (fragmentActivity == null) {
            if (BuildConfig.DEBUG) {
                throw RuntimeException("No activity to use")
            }
        }
    }

    override fun onViewCreated(view: View, savedState: Bundle?) {
        super.onViewCreated(view, savedState)

        setupBottomBar(view)

        displayUrlView = view.findViewById(R.id.display_url)
        displayUrlView.setOnClickListener {
            chromeViewModel.showUrlInput.setValue(chromeViewModel.currentUrl.value)
        }

        siteIdentity = view.findViewById(R.id.site_identity)

        browserContainer = view.findViewById(R.id.browser_container)
        videoContainer = view.findViewById(R.id.video_container)
        tabViewSlot = view.findViewById(R.id.tab_view_slot)
        progressView = view.findViewById(R.id.progress)

        initTrackerView(view)

        monitorTrackerBlocked { count -> updateTrackerBlockedCount(count) }

        view.findViewById<View>(R.id.appbar).setOnApplyWindowInsetsListener { v, insets ->
            (v.layoutParams as LinearLayout.LayoutParams).topMargin = insets.systemWindowInsetTop
            insets
        }

        toolbarRoot = view.findViewById(R.id.toolbar_root)

        sessionManager = TabsSessionProvider.getOrThrow(activity)
        observer = Observer(this)
        sessionManager.register(observer)
        sessionManager.focusSession?.register(observer)

        observeChromeAction()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        sessionManager.focusSession?.unregister(observer)
        sessionManager.unregister(observer)
    }

    override fun onResume() {
        super.onResume()
        sessionManager.resume()
    }

    override fun onPause() {
        super.onPause()
        sessionManager.pause()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        permissionHandler = PermissionHandler(object : PermissionHandle {
            override fun doActionDirect(permission: String?, actionId: Int, params: Parcelable?) {

                this@BrowserFragmentLegacy.context?.also {
                    val download = params as Download

                    if (PackageManager.PERMISSION_GRANTED ==
                            ContextCompat.checkSelfPermission(it, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    ) {
                        // We do have the permission to write to the external storage. Proceed with the download.
                        queueDownload(download)
                    }
                } ?: run {
                    Log.e("BrowserFragment.kt", "No context to use, abort callback onDownloadStart")
                }
            }

            fun actionDownloadGranted(parcelable: Parcelable?) {
                val download = parcelable as Download
                queueDownload(download)
            }

            override fun doActionGranted(permission: String?, actionId: Int, params: Parcelable?) {
                actionDownloadGranted(params)
            }

            override fun doActionSetting(permission: String?, actionId: Int, params: Parcelable?) {
                actionDownloadGranted(params)
            }

            override fun doActionNoPermission(
                permission: String?,
                actionId: Int,
                params: Parcelable?
            ) {
            }

            override fun makeAskAgainSnackBar(actionId: Int): Snackbar {
                activity?.also {
                    return PermissionHandler.makeAskAgainSnackBar(
                            this@BrowserFragmentLegacy,
                            it.findViewById(R.id.container),
                            R.string.permission_toast_storage
                    )
                }
                throw IllegalStateException("No Activity to show Snackbar.")
            }

            override fun permissionDeniedToast(actionId: Int) {
                Toast.makeText(getContext(), R.string.permission_toast_storage_deny, Toast.LENGTH_LONG).show()
            }

            override fun requestPermissions(actionId: Int) {
                this@BrowserFragmentLegacy.requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), actionId)
            }

            private fun queueDownload(download: Download?) {
                activity?.let { activity ->
                    download?.let {
                        EnqueueDownloadTask(activity, it, displayUrlView.text.toString()).execute()
                    }
                }
            }
        })
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        trackerPopup.dismiss()

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            toolbarRoot.visibility = View.GONE
            browser_bottom_bar.visibility = View.GONE
        } else {
            browser_bottom_bar.visibility = View.VISIBLE
            toolbarRoot.visibility = View.VISIBLE
        }

        refreshVideoContainer()
    }

    // Workaround for full-screen WebView issue that the video doesn't fit the viewport
    // after rotating the device from portrait to landscape and vice versa. It could reduce
    // the issue happened rate by changing the video view layout size to a slight smaller size
    // then add to the full screen size again when the device is rotated.
    private fun refreshVideoContainer() {
        if (videoContainer.visibility == View.VISIBLE) {
            updateVideoContainerWithLayoutParams(FrameLayout.LayoutParams(
                (videoContainer.height * 0.99).toInt(),
                (videoContainer.width * 0.99).toInt()
            ))
            videoContainer.post {
                if (videoContainer.visibility == View.VISIBLE) {
                    updateVideoContainerWithLayoutParams(FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    ))
                }
            }
        }
    }

    private fun updateVideoContainerWithLayoutParams(params: FrameLayout.LayoutParams) {
        val fullscreenContentView: View? = videoContainer.getChildAt(0)
        if (fullscreenContentView != null) {
            videoContainer.removeAllViews()
            videoContainer.addView(fullscreenContentView, params)
        }
    }

    override fun applyLocale() {
        // We create and destroy a new WebView here to force the internal state of WebView to know
        // about the new language. See issue #666.
        val unneeded = WebView(context)
        unneeded.destroy()
    }

    override fun onBackPressed(): Boolean {
        val focus = sessionManager.focusSession ?: return false
        val tabView = focus.engineSession?.tabView ?: return false

        // After we apply the full screen rotation workaround - 'refreshVideoContainer',
        // it may not be able to get 'onExitFullScreen' callback from WebChromeClient. Just call it here
        // to leave the full screen mode.
        if (videoContainer.isVisible) {
            observer.onExitFullScreen()
            return true
        }

        if (tabView.canGoBack()) {
            goBack()
            return true
        }

        sessionManager.dropTab(focus.id)
        ScreenNavigator.get(activity).popToHomeScreen(true)
        chromeViewModel.dropCurrentPage.call()
        return true
    }

    override fun getFragment(): Fragment {
        return this
    }

    override fun switchToTab(tabId: String) {
        if (!TextUtils.isEmpty(tabId)) {
            sessionManager.switchToTab(tabId)
        }
    }

    override fun goForeground() {
        val tabView = sessionManager.focusSession?.engineSession?.tabView ?: return
        if (tabViewSlot.childCount == 0) {
            tabViewSlot.addView(tabView.view)
        }
    }

    override fun goBackground() {
        val focus = sessionManager.focusSession ?: return
        val tabView = focus.engineSession?.tabView ?: return
        focus.engineSession?.detach()
        tabViewSlot.removeView(tabView.view)
    }

    override fun loadUrl(
        url: String,
        openNewTab: Boolean,
        isFromExternal: Boolean,
        onViewReadyCallback: Runnable?
    ) {
        if (url.isNotBlank()) {
            displayUrlView.text = url
            if (sessionManager.tabsCount == 0) {
                sessionManager.addTab(url, TabUtil.argument(null, false, true))
            } else {
                sessionManager.focusSession!!.engineSession?.tabView?.loadUrl(url)
            }

            ThreadUtils.postToMainThread(onViewReadyCallback)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        permissionHandler.onRequestPermissionsResult(context, requestCode, permissions, grantResults)
    }

    private fun goBack() = sessionManager.focusSession?.engineSession?.goBack()
    private fun goForward() = sessionManager.focusSession?.engineSession?.goForward()
    private fun stop() = sessionManager.focusSession?.engineSession?.stopLoading()
    private fun reload() = sessionManager.focusSession?.engineSession?.reload()

    private fun onTrackerButtonClicked() {
        view?.let { parentView -> trackerPopup.show(parentView) }
    }

    private fun onDeleteClicked() {
        for (tab in sessionManager.getTabs()) {
            sessionManager.dropTab(tab.id)
        }
        chromeViewModel.dropCurrentPage.call()
        ScreenNavigator.get(activity).popToHomeScreen(true)
    }

    private fun setupBottomBar(rootView: View) {
        val bottomBar = rootView.findViewById<BottomBar>(R.id.browser_bottom_bar)
        bottomBar.setOnItemClickListener(object : BottomBar.OnItemClickListener {
            override fun onItemClick(type: Int, position: Int) {
                when (type) {
                    BottomBarItemAdapter.TYPE_SEARCH -> chromeViewModel.showUrlInput.setValue(chromeViewModel.currentUrl.value)
                    BottomBarItemAdapter.TYPE_PIN_SHORTCUT -> chromeViewModel.pinShortcut.call()
                    BottomBarItemAdapter.TYPE_REFRESH -> chromeViewModel.refreshOrStop.call()
                    BottomBarItemAdapter.TYPE_SHARE -> chromeViewModel.share.call()
                    BottomBarItemAdapter.TYPE_NEXT -> chromeViewModel.goNext.call()
                    BottomBarItemAdapter.TYPE_PRIVATE_HOME -> {
                        chromeViewModel.togglePrivateMode.call()
                        TelemetryWrapper.togglePrivateMode(true)
                    }
                    BottomBarItemAdapter.TYPE_DELETE -> onDeleteClicked()
                    BottomBarItemAdapter.TYPE_TRACKER -> onTrackerButtonClicked()
                    else -> throw IllegalArgumentException("Unhandled bottom bar item, type: $type")
                }
            }
        })
        bottomBarItemAdapter = BottomBarItemAdapter(bottomBar, BottomBarItemAdapter.Theme.PrivateMode)
        val bottomBarViewModel = getActivityViewModel(privateBottomBarViewModelCreator)
        bottomBarViewModel.items.nonNullObserve(this) {
            bottomBarItemAdapter.setItems(it)
            bottomBarItemAdapter.endPrivateHomeAnimation()
            bottomBarItemAdapter.setTrackerSwitch(isTurboModeEnabled(rootView.context))
        }

        chromeViewModel.isRefreshing.switchFrom(bottomBarViewModel.items)
                .observe(viewLifecycleOwner, Observer { bottomBarItemAdapter.setRefreshing(it == true) })
        chromeViewModel.canGoForward.switchFrom(bottomBarViewModel.items)
                .observe(viewLifecycleOwner, Observer { bottomBarItemAdapter.setCanGoForward(it == true) })
    }

    private fun initTrackerView(parentView: View) {
        trackerPopup = TrackerPopup(parentView.context)

        trackerPopup.onSwitchToggled = { enabled ->
            val appContext = (parentView.context.applicationContext as FocusApplication)
            appContext.settings.privateBrowsingSettings.setTurboMode(enabled)
            sessionManager.focusSession?.engineSession?.tabView?.setContentBlockingEnabled(enabled)

            bottomBarItemAdapter.setTrackerSwitch(enabled)
            stop()
            reload()
        }
    }

    private fun isTurboModeEnabled(context: Context): Boolean {
        val appContext = (context.applicationContext as FocusApplication)
        return appContext.settings.privateBrowsingSettings.shouldUseTurboMode()
    }

    private fun monitorTrackerBlocked(onUpdate: (Int) -> Unit) {
        BrowsingSession.getInstance().blockedTrackerCount.observe(viewLifecycleOwner, Observer {
            val count = it ?: return@Observer
            onUpdate(count)
        })
    }

    private fun updateTrackerBlockedCount(count: Int) {
        bottomBarItemAdapter.setTrackerBadgeEnabled(count > 0)
        trackerPopup.blockedCount = count
    }

    private fun observeChromeAction() {
        chromeViewModel.refreshOrStop.observe(viewLifecycleOwner, Observer {
            if (chromeViewModel.isRefreshing.value == true) {
                stop()
            } else {
                reload()
            }
        })
        chromeViewModel.goNext.observe(viewLifecycleOwner, Observer {
            if (chromeViewModel.canGoForward.value == true) {
                goForward()
            }
        })
    }

    class Observer(val fragment: BrowserFragmentLegacy) : SessionManager.Observer, Session.Observer {
        override fun updateFailingUrl(url: String?, updateFromError: Boolean) {
            // do nothing, exist for interface compatibility only.
        }

        override fun handleExternalUrl(url: String?): Boolean {
            return fragment.context?.let {
                IntentUtils.handleExternalUri(it, url)
            } ?: false
        }

        override fun onShowFileChooser(
            es: TabViewEngineSession,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: WebChromeClient.FileChooserParams?
        ): Boolean {
            // do nothing, exist for interface compatibility only.
            return false
        }

        var fullscreenCallback: FullscreenCallback? = null
        var session: Session? = null

        override fun onSessionAdded(session: Session, arguments: Bundle?) {
        }

        override fun onProgress(session: Session, progress: Int) {
            fragment.progressView.progress = progress
        }

        override fun onTitleChanged(session: Session, title: String?) {
            fragment.chromeViewModel.onFocusedTitleChanged(title)
            session.let {
                if (fragment.displayUrlView.text.toString() != it.url) {
                    fragment.displayUrlView.text = it.url
                }
            }
        }

        override fun onLongPress(session: Session, hitTarget: HitTarget) {
            fragment.activity?.let {
                WebContextMenu.show(true,
                        it,
                        PrivateDownloadCallback(fragment, session.url),
                        hitTarget)
            }
        }

        override fun onEnterFullScreen(callback: FullscreenCallback, view: View?) {
            with(fragment) {
                browserContainer.visibility = View.INVISIBLE
                videoContainer.visibility = View.VISIBLE
                videoContainer.addView(view)

                // Switch to immersive mode: Hide system bars other UI controls
                systemVisibility = ViewUtils.switchToImmersiveMode(activity)
            }

            fullscreenCallback = callback
        }

        override fun onExitFullScreen() {
            with(fragment) {
                browserContainer.visibility = View.VISIBLE
                videoContainer.visibility = View.GONE
                videoContainer.removeAllViews()

                if (systemVisibility != ViewUtils.SYSTEM_UI_VISIBILITY_NONE) {
                    ViewUtils.exitImmersiveMode(systemVisibility, activity)
                }
            }

            fullscreenCallback?.fullScreenExited()
            fullscreenCallback = null

            // WebView gets focus, but unable to open the keyboard after exit Fullscreen for Android 7.0+
            // We guess some component in WebView might lock focus
            // So when user touches the input text box on Webview, it will not trigger to open the keyboard
            // It may be a WebView bug.
            // The workaround is clearing WebView focus
            // The WebView will be normal when it gets focus again.
            // If android change behavior after, can remove this.
            session?.engineSession?.tabView?.let { if (it is WebView) it.clearFocus() }
        }

        override fun onUrlChanged(session: Session, url: String?) {
            fragment.chromeViewModel.onFocusedUrlChanged(url)
            if (!UrlUtils.isInternalErrorURL(url)) {
                fragment.displayUrlView.text = url
            }
        }

        override fun onLoadingStateChanged(session: Session, loading: Boolean) {
            if (loading) {
                fragment.chromeViewModel.onPageLoadingStarted()
            } else {
                fragment.chromeViewModel.onPageLoadingStopped()
            }
        }

        override fun onSecurityChanged(session: Session, isSecure: Boolean) {
            val level = if (isSecure) SITE_LOCK else SITE_GLOBE
            fragment.siteIdentity.setImageLevel(level)
        }

        override fun onSessionCountChanged(count: Int) {
            fragment.chromeViewModel.onTabCountChanged(count)
            if (count == 0) {
                session?.unregister(this)
            } else {
                session = fragment.sessionManager.focusSession
                session?.register(this)
            }
        }

        override fun onDownload(
            session: Session,
            download: mozilla.components.browser.session.Download
        ): Boolean {
            val activity = fragment.activity
            if (activity == null || !activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                return false
            }

            val d = Download(
                    download.url,
                    download.fileName,
                    download.userAgent,
                    "",
                    download.contentType,
                    download.contentLength!!,
                    false
                    )
            fragment.permissionHandler.tryAction(
                    fragment,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    ACTION_DOWNLOAD,
                    d
                    )
            return true
        }

        override fun onHttpAuthRequest(
            callback: TabViewClient.HttpAuthCallback,
            host: String?,
            realm: String?
        ) {
            val builder = HttpAuthenticationDialogBuilder.Builder(fragment.activity, host, realm)
                    .setOkListener { _, _, username, password -> callback.proceed(username, password) }
                    .setCancelListener { callback.cancel() }
                    .build()

            builder.createDialog()
            builder.show()
        }

        override fun onNavigationStateChanged(session: Session, canGoBack: Boolean, canGoForward: Boolean) {
            fragment.chromeViewModel.onNavigationStateChanged(canGoBack, canGoForward)
        }

        override fun onFocusChanged(session: Session?, factor: SessionManager.Factor) {
            fragment.chromeViewModel.onFocusedUrlChanged(session?.url)
            fragment.chromeViewModel.onFocusedTitleChanged(session?.title)
            if (session != null) {
                val canGoBack = fragment.sessionManager.focusSession?.canGoBack ?: false
                val canGoForward = fragment.sessionManager.focusSession?.canGoForward ?: false
                fragment.chromeViewModel.onNavigationStateChanged(canGoBack, canGoForward)
            }
        }
    }

    class PrivateDownloadCallback(val fragment: BrowserFragmentLegacy, val refererUrl: String?) : DownloadCallback {
        override fun onDownloadStart(download: Download) {
            fragment.activity?.let {
                if (!it.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    return
                }
            }

            fragment.permissionHandler.tryAction(
                    fragment,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    ACTION_DOWNLOAD,
                    download
                    )
        }
    }
}
