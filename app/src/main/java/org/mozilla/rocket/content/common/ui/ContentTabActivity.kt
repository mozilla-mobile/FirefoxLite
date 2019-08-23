package org.mozilla.rocket.content.common.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.snackbar.Snackbar
import dagger.Lazy
import org.mozilla.focus.R
import org.mozilla.focus.activity.BaseActivity
import org.mozilla.focus.download.DownloadInfoManager
import org.mozilla.focus.download.EnqueueDownloadTask
import org.mozilla.focus.menu.WebContextMenu
import org.mozilla.focus.utils.Constants
import org.mozilla.focus.utils.IntentUtils
import org.mozilla.focus.utils.ViewUtils
import org.mozilla.focus.web.HttpAuthenticationDialogBuilder
import org.mozilla.focus.widget.AnimatedProgressBar
import org.mozilla.focus.widget.BackKeyHandleable
import org.mozilla.permissionhandler.PermissionHandle
import org.mozilla.permissionhandler.PermissionHandler
import org.mozilla.rocket.chrome.BottomBarItemAdapter
import org.mozilla.rocket.chrome.ChromeViewModel
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.getViewModel
import org.mozilla.rocket.content.view.BottomBar
import org.mozilla.rocket.extension.nonNullObserve
import org.mozilla.rocket.extension.switchFrom
import org.mozilla.rocket.privately.PrivateTabViewProvider
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

class ContentTabActivity : BaseActivity(), TabsSessionProvider.SessionHost {

    @Inject
    lateinit var chromeViewModelCreator: Lazy<ChromeViewModel>

    @Inject
    lateinit var bottomBarViewModelCreator: Lazy<ContentTabBottomBarViewModel>

    private lateinit var permissionHandler: PermissionHandler
    private lateinit var chromeViewModel: ChromeViewModel
    private lateinit var tabViewProvider: PrivateTabViewProvider
    private lateinit var sessionManager: SessionManager
    private lateinit var observer: Observer
    private lateinit var uiMessageReceiver: BroadcastReceiver
    private lateinit var bottomBarItemAdapter: BottomBarItemAdapter
    private lateinit var displayUrlView: TextView
    private lateinit var progressView: AnimatedProgressBar
    private lateinit var siteIdentity: ImageView
    private lateinit var browserContainer: ViewGroup
    private lateinit var videoContainer: ViewGroup
    private lateinit var toolbarRoot: ViewGroup
    private lateinit var bottomBar: BottomBar
    private lateinit var snackBarContainer: View

    private var systemVisibility = ViewUtils.SYSTEM_UI_VISIBILITY_NONE

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_content_tab)

        chromeViewModel = getViewModel(chromeViewModelCreator)
        tabViewProvider = PrivateTabViewProvider(this)
        sessionManager = SessionManager(tabViewProvider)

        displayUrlView = findViewById(R.id.display_url)
        siteIdentity = findViewById(R.id.site_identity)
        browserContainer = findViewById(R.id.fragment_container)
        videoContainer = findViewById(R.id.video_container)
        progressView = findViewById(R.id.progress)
        findViewById<View>(R.id.appbar).setOnApplyWindowInsetsListener { v, insets ->
            (v.layoutParams as ConstraintLayout.LayoutParams).topMargin = insets.systemWindowInsetTop
            insets
        }
        toolbarRoot = findViewById(R.id.toolbar_root)
        snackBarContainer = findViewById(R.id.snack_bar_container)
        makeStatusBarTransparent()
        bottomBar = findViewById(R.id.bottom_bar)
        setupBottomBar(bottomBar)

        initBroadcastReceivers()
        initPermissionHandler()

        observer = Observer(this)
        sessionManager.register(observer)

        observeChromeAction()
        chromeViewModel.showUrlInput.value = chromeViewModel.currentUrl.value

        if (savedInstanceState == null) {
            val url = intent?.extras?.getString(EXTRA_URL) ?: ""
            val enableTurboMode = intent?.extras?.getBoolean(EXTRA_ENABLE_TURBO_MODE) ?: true
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ContentTabFragment.newInstance(url, enableTurboMode))
                .commit()
        }
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
        sessionManager.unregister(observer)
        sessionManager.destroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            toolbarRoot.visibility = View.GONE
            bottomBar.visibility = View.GONE
        } else {
            toolbarRoot.visibility = View.VISIBLE
            bottomBar.visibility = View.VISIBLE
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        permissionHandler.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }

    override fun applyLocale() {}

    override fun onBackPressed() {
        if (supportFragmentManager.isStateSaved) {
            return
        }

        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment != null && fragment is BackKeyHandleable) {
            val handled = fragment.onBackPressed()
            if (handled) {
                return
            }
        }

        super.onBackPressed()
    }

    override fun getSessionManager(): SessionManager {
        return sessionManager
    }

    private fun setupBottomBar(bottomBar: BottomBar) {
        bottomBar.setOnItemClickListener(object : BottomBar.OnItemClickListener {
            override fun onItemClick(type: Int, position: Int) {
                when (type) {
                    BottomBarItemAdapter.TYPE_BACK -> onBackPressed()
                    BottomBarItemAdapter.TYPE_REFRESH -> chromeViewModel.refreshOrStop.call()
                    BottomBarItemAdapter.TYPE_SHARE -> chromeViewModel.share.call()
                    BottomBarItemAdapter.TYPE_OPEN_IN_NEW_TAB -> {
                        startActivity(
                            IntentUtils.createInternalOpenUrlIntent(
                                this@ContentTabActivity,
                                chromeViewModel.currentUrl.value,
                                true
                            )
                        )
                    }
                    else -> throw IllegalArgumentException("Unhandled bottom bar item, type: $type")
                }
            }
        })
        bottomBarItemAdapter = BottomBarItemAdapter(bottomBar, BottomBarItemAdapter.Theme.PrivateMode)
        val bottomBarViewModel = getViewModel(bottomBarViewModelCreator)
        bottomBarViewModel.items.nonNullObserve(this) {
            bottomBarItemAdapter.setItems(it)
        }

        chromeViewModel.isRefreshing.switchFrom(bottomBarViewModel.items)
            .observe(this, Observer { bottomBarItemAdapter.setRefreshing(it == true) })
        chromeViewModel.canGoForward.switchFrom(bottomBarViewModel.items)
            .observe(this, Observer { bottomBarItemAdapter.setCanGoForward(it == true) })
    }

    private fun makeStatusBarTransparent() {
        var visibility = window.decorView.systemUiVisibility
        // do not overwrite existing value
        visibility = visibility or (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        window.decorView.systemUiVisibility = visibility
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

    private fun initPermissionHandler() {
        permissionHandler = PermissionHandler(object : PermissionHandle {
            override fun doActionDirect(permission: String?, actionId: Int, params: Parcelable?) {

                this@ContentTabActivity.also {
                    val download = params as Download

                    if (PackageManager.PERMISSION_GRANTED ==
                        ContextCompat.checkSelfPermission(it, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    ) {
                        // We do have the permission to write to the external storage. Proceed with the download.
                        queueDownload(download)
                    }
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
                this@ContentTabActivity.also {
                    return PermissionHandler.makeAskAgainSnackBar(
                        this@ContentTabActivity,
                        it.findViewById(R.id.container),
                        R.string.permission_toast_storage
                    )
                }
            }

            override fun permissionDeniedToast(actionId: Int) {
                Toast.makeText(this@ContentTabActivity, R.string.permission_toast_storage_deny, Toast.LENGTH_LONG).show()
            }

            override fun requestPermissions(actionId: Int) {
                ActivityCompat.requestPermissions(this@ContentTabActivity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), actionId)
            }

            private fun queueDownload(download: Download?) {
                this@ContentTabActivity.let { activity ->
                    download?.let {
                        EnqueueDownloadTask(activity, it, displayUrlView.text.toString()).execute()
                    }
                }
            }
        })
    }

    private fun observeChromeAction() {
        chromeViewModel.share.observe(this, Observer {
            chromeViewModel.currentUrl.value?.let { url ->
                onShareClicked(url)
            }
        })
    }

    private fun onShareClicked(url: String) {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, url)
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_dialog_title)))
    }

    companion object {
        private const val LOG_TAG = "ContentTabActivity"
        private const val EXTRA_URL = "url"
        private const val EXTRA_ENABLE_TURBO_MODE = "enable_turbo_mode"

        fun getStartIntent(context: Context, url: String, enableTurboMode: Boolean = true) =
            Intent(context, ContentTabActivity::class.java).also {
                it.putExtra(EXTRA_URL, url)
                it.putExtra(EXTRA_ENABLE_TURBO_MODE, enableTurboMode)
            }
    }

    class Observer(val activity: ContentTabActivity) : SessionManager.Observer, Session.Observer {
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
            activity.progressView.progress = progress
        }

        override fun onTitleChanged(session: Session, title: String?) {
            activity.chromeViewModel.onFocusedTitleChanged(title)
            session.let {
                if (activity.displayUrlView.text.toString() != it.url) {
                    activity.displayUrlView.text = it.url
                }
            }
        }

        override fun onLongPress(session: Session, hitTarget: TabView.HitTarget) {
            activity.let {
                WebContextMenu.show(true,
                    it,
                    DownloadCallback(activity, session.url),
                    hitTarget)
            }
        }

        override fun onEnterFullScreen(callback: TabView.FullscreenCallback, view: View?) {
            with(activity) {
                toolbarRoot.visibility = View.GONE
                bottomBar.visibility = View.GONE
                browserContainer.visibility = View.INVISIBLE
                videoContainer.visibility = View.VISIBLE
                videoContainer.addView(view)

                // Switch to immersive mode: Hide system bars other UI controls
                systemVisibility = ViewUtils.switchToImmersiveMode(activity)
            }
        }

        override fun onExitFullScreen() {
            with(activity) {
                toolbarRoot.visibility = View.VISIBLE
                bottomBar.visibility = View.VISIBLE
                browserContainer.visibility = View.VISIBLE
                videoContainer.visibility = View.INVISIBLE
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
            activity.chromeViewModel.onFocusedUrlChanged(url)
            if (!UrlUtils.isInternalErrorURL(url)) {
                activity.displayUrlView.text = url
            }
        }

        override fun onLoadingStateChanged(session: Session, loading: Boolean) {
            if (loading) {
                activity.chromeViewModel.onPageLoadingStarted()
            } else {
                activity.chromeViewModel.onPageLoadingStopped()
            }
        }

        override fun onSecurityChanged(session: Session, isSecure: Boolean) {
            val level = if (isSecure) SITE_LOCK else SITE_GLOBE
            activity.siteIdentity.setImageLevel(level)
        }

        override fun onSessionCountChanged(count: Int) {
            activity.chromeViewModel.onTabCountChanged(count)
            if (count == 0) {
                session?.unregister(this)
            } else {
                session = activity.sessionManager.focusSession
                session?.register(this)
            }
        }

        override fun onDownload(
            session: Session,
            download: mozilla.components.browser.session.Download
        ): Boolean {
            if (!activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
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
            activity.permissionHandler.tryAction(
                activity,
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
            val builder = HttpAuthenticationDialogBuilder.Builder(activity, host, realm)
                .setOkListener { _, _, username, password -> callback.proceed(username, password) }
                .setCancelListener { callback.cancel() }
                .build()

            builder.createDialog()
            builder.show()
        }

        override fun onNavigationStateChanged(session: Session, canGoBack: Boolean, canGoForward: Boolean) {
            activity.chromeViewModel.onNavigationStateChanged(canGoBack, canGoForward)
        }

        override fun onFocusChanged(session: Session?, factor: SessionManager.Factor) {
            activity.chromeViewModel.onFocusedUrlChanged(session?.url)
            activity.chromeViewModel.onFocusedTitleChanged(session?.title)
            if (session != null) {
                val canGoBack = activity.sessionManager.focusSession?.canGoBack ?: false
                val canGoForward = activity.sessionManager.focusSession?.canGoForward ?: false
                activity.chromeViewModel.onNavigationStateChanged(canGoBack, canGoForward)
            }
        }
    }

    private class DownloadCallback(val activity: ContentTabActivity, val refererUrl: String?) : org.mozilla.rocket.tabs.web.DownloadCallback {
        override fun onDownloadStart(download: Download) {
            activity.let {
                if (!it.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    return
                }
            }

            activity.permissionHandler.tryAction(
                activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ACTION_DOWNLOAD,
                download
            )
        }
    }
}
