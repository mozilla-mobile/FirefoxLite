package org.mozilla.rocket.shopping.search.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import androidx.viewpager.widget.ViewPager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import dagger.Lazy
import org.mozilla.focus.R
import org.mozilla.focus.download.EnqueueDownloadTask
import org.mozilla.focus.menu.WebContextMenu
import org.mozilla.focus.utils.ViewUtils
import org.mozilla.focus.web.HttpAuthenticationDialogBuilder
import org.mozilla.focus.widget.AnimatedProgressBar
import org.mozilla.permissionhandler.PermissionHandle
import org.mozilla.permissionhandler.PermissionHandler
import org.mozilla.rocket.chrome.BottomBarItemAdapter
import org.mozilla.rocket.chrome.ChromeViewModel
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.common.ui.ContentTabFragment
import org.mozilla.rocket.content.getActivityViewModel
import org.mozilla.rocket.content.getViewModel
import org.mozilla.rocket.content.view.BottomBar
import org.mozilla.rocket.extension.nonNullObserve
import org.mozilla.rocket.extension.switchFrom
import org.mozilla.rocket.shopping.search.ui.ShoppingSearchTabsAdapter.TabItem
import org.mozilla.rocket.tabs.Session
import org.mozilla.rocket.tabs.SessionManager
import org.mozilla.rocket.tabs.TabView
import org.mozilla.rocket.tabs.TabViewClient
import org.mozilla.rocket.tabs.TabViewEngineSession
import org.mozilla.rocket.tabs.TabsSessionProvider
import org.mozilla.rocket.tabs.web.Download
import org.mozilla.urlutils.UrlUtils
import javax.inject.Inject

private const val SITE_GLOBE = 0
private const val SITE_LOCK = 1
private const val ACTION_DOWNLOAD = 0

class ShoppingSearchResultTabFragment : Fragment() {

    @Inject
    lateinit var viewModelCreator: Lazy<ShoppingSearchResultViewModel>

    @Inject
    lateinit var chromeViewModelCreator: Lazy<ChromeViewModel>

    @Inject
    lateinit var bottomBarViewModelCreator: Lazy<ShoppingSearchBottomBarViewModel>

    private lateinit var shoppingSearchResultViewModel: ShoppingSearchResultViewModel
    private lateinit var chromeViewModel: ChromeViewModel
    private lateinit var permissionHandler: PermissionHandler
    private lateinit var sessionManager: SessionManager
    private lateinit var observer: Observer
    private lateinit var bottomBarItemAdapter: BottomBarItemAdapter

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager
    private lateinit var bottomBar: BottomBar
    private lateinit var videoContainer: ViewGroup
    private lateinit var displayUrlView: TextView
    private lateinit var progressView: AnimatedProgressBar
    private lateinit var siteIdentity: ImageView
    private lateinit var toolbarRoot: ViewGroup

    private val safeArgs: ShoppingSearchResultTabFragmentArgs by navArgs()
    private val searchKeyword by lazy { safeArgs.searchKeyword }

    private var systemVisibility = ViewUtils.SYSTEM_UI_VISIBILITY_NONE

    override fun onAttach(context: Context) {
        super.onAttach(context)
        initPermissionHandler()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        shoppingSearchResultViewModel = getViewModel(viewModelCreator)
        chromeViewModel = getActivityViewModel(chromeViewModelCreator)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_shopping_search_result_tab, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBottomBar(view)

        tabLayout = view.findViewById(R.id.shopping_search_tabs)
        viewPager = view.findViewById(R.id.view_pager)
        bottomBar = view.findViewById(R.id.bottom_bar)
        videoContainer = view.findViewById(R.id.video_container)
        displayUrlView = view.findViewById(R.id.display_url)
        siteIdentity = view.findViewById(R.id.site_identity)
        progressView = view.findViewById(R.id.progress)
        toolbarRoot = view.findViewById(R.id.toolbar_root)

        view.findViewById<View>(R.id.appbar).setOnApplyWindowInsetsListener { v, insets ->
            (v.layoutParams as LinearLayout.LayoutParams).topMargin = insets.systemWindowInsetTop
            insets
        }

        sessionManager = TabsSessionProvider.getOrThrow(activity)
        observer = Observer(this)
        sessionManager.register(observer)
        sessionManager.focusSession?.register(observer)

        observeChromeAction()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initViewPager()
        initTabLayout()

        shoppingSearchResultViewModel.search(searchKeyword)
    }

    private fun initPermissionHandler() {
        permissionHandler = PermissionHandler(object : PermissionHandle {
            override fun doActionDirect(permission: String?, actionId: Int, params: Parcelable?) {

                this@ShoppingSearchResultTabFragment.context?.also {
                    val download = params as Download

                    if (PackageManager.PERMISSION_GRANTED ==
                        ContextCompat.checkSelfPermission(it, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    ) {
                        // We do have the permission to write to the external storage. Proceed with the download.
                        queueDownload(download)
                    }
                } ?: run {
                    Log.e("ShoppingSearch", "No context to use, abort callback onDownloadStart")
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
                        this@ShoppingSearchResultTabFragment,
                        it.findViewById(R.id.container),
                        R.string.permission_toast_storage
                    )
                }
                throw IllegalStateException("No Activity to show Snackbar.")
            }

            override fun permissionDeniedToast(actionId: Int) {
                Toast.makeText(context, R.string.permission_toast_storage_deny, Toast.LENGTH_LONG).show()
            }

            override fun requestPermissions(actionId: Int) {
                this@ShoppingSearchResultTabFragment.requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), actionId)
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

    private fun setupBottomBar(rootView: View) {
        val bottomBar = rootView.findViewById<BottomBar>(R.id.bottom_bar)
        bottomBar.setOnItemClickListener(object : BottomBar.OnItemClickListener {
            override fun onItemClick(type: Int, position: Int) {
                when (type) {
                    BottomBarItemAdapter.TYPE_PRIVATE_HOME -> activity?.onBackPressed()
                    BottomBarItemAdapter.TYPE_REFRESH -> chromeViewModel.refreshOrStop.call()
                    BottomBarItemAdapter.TYPE_DELETE -> activity?.finishAndRemoveTask()
                    BottomBarItemAdapter.TYPE_NEXT -> chromeViewModel.goNext.call()
                    BottomBarItemAdapter.TYPE_SHARE -> chromeViewModel.share.call()
                    else -> throw IllegalArgumentException("Unhandled bottom bar item, type: $type")
                }
            }
        })
        bottomBarItemAdapter = BottomBarItemAdapter(bottomBar, BottomBarItemAdapter.Theme.Light)
        val bottomBarViewModel = getActivityViewModel(bottomBarViewModelCreator)
        bottomBarViewModel.items.nonNullObserve(this) {
            bottomBarItemAdapter.setItems(it)
        }

        chromeViewModel.isRefreshing.switchFrom(bottomBarViewModel.items)
            .observe(this, Observer { bottomBarItemAdapter.setRefreshing(it == true) })
        chromeViewModel.canGoForward.switchFrom(bottomBarViewModel.items)
            .observe(this, Observer { bottomBarItemAdapter.setCanGoForward(it == true) })
    }

    private fun observeChromeAction() {
        chromeViewModel.refreshOrStop.observe(this, Observer {
            if (chromeViewModel.isRefreshing.value == true) {
                stop()
            } else {
                reload()
            }
        })
        chromeViewModel.goNext.observe(this, Observer {
            if (chromeViewModel.canGoForward.value == true) {
                goForward()
            }
        })
    }

    private fun goBack() = sessionManager.focusSession?.engineSession?.goBack()

    private fun goForward() = sessionManager.focusSession?.engineSession?.goForward()

    private fun stop() = sessionManager.focusSession?.engineSession?.stopLoading()

    private fun reload() = sessionManager.focusSession?.engineSession?.reload()

    private fun initViewPager() {
        shoppingSearchResultViewModel.uiModel.observe(this, Observer { uiModel ->
            val tabItems = uiModel.sites.map { site ->
                TabItem(
                    ContentTabFragment.newInstance(site.searchUrl),
                    site.title
                )
            }
            viewPager.adapter = ShoppingSearchTabsAdapter(childFragmentManager, tabItems)
        })
    }

    private fun initTabLayout() {
        tabLayout.setupWithViewPager(viewPager)
    }

    class Observer(val fragment: ShoppingSearchResultTabFragment) : SessionManager.Observer, Session.Observer {
        override fun updateFailingUrl(url: String?, updateFromError: Boolean) {
            // do nothing, exist for interface compatibility only.
        }

        override fun handleExternalUrl(url: String?): Boolean {
            // do nothing, exist for interface compatibility only.
            return false
        }

        override fun onShowFileChooser(
            es: TabViewEngineSession,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: WebChromeClient.FileChooserParams?
        ): Boolean {
            // do nothing, exist for interface compatibility only.
            return false
        }

        var callback: TabView.FullscreenCallback? = null
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

        override fun onLongPress(session: Session, hitTarget: TabView.HitTarget) {
            fragment.activity?.let {
                WebContextMenu.show(true,
                    it,
                    DownloadCallback(fragment, session.url),
                    hitTarget)
            }
        }

        override fun onEnterFullScreen(callback: TabView.FullscreenCallback, view: View?) {
            with(fragment) {
                toolbarRoot.visibility = View.GONE
                bottomBar.visibility = View.GONE
                tabLayout.visibility = View.GONE
                viewPager.visibility = View.INVISIBLE
                videoContainer.visibility = View.VISIBLE
                videoContainer.addView(view)

                // Switch to immersive mode: Hide system bars other UI controls
                systemVisibility = ViewUtils.switchToImmersiveMode(activity)
            }
        }

        override fun onExitFullScreen() {
            with(fragment) {
                toolbarRoot.visibility = View.VISIBLE
                bottomBar.visibility = View.VISIBLE
                tabLayout.visibility = View.VISIBLE
                viewPager.visibility = View.VISIBLE
                videoContainer.visibility = View.GONE
                videoContainer.removeAllViews()

                if (systemVisibility != ViewUtils.SYSTEM_UI_VISIBILITY_NONE) {
                    ViewUtils.exitImmersiveMode(systemVisibility, activity)
                }
            }

            callback?.fullScreenExited()
            callback = null

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

    private class DownloadCallback(val fragment: ShoppingSearchResultTabFragment, val refererUrl: String?) : org.mozilla.rocket.tabs.web.DownloadCallback {
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
