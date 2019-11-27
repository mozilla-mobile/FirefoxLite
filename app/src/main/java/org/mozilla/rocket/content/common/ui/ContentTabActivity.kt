package org.mozilla.rocket.content.common.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import dagger.Lazy
import kotlinx.android.synthetic.main.activity_content_tab.*
import kotlinx.android.synthetic.main.toolbar.*
import org.mozilla.focus.R
import org.mozilla.focus.activity.BaseActivity
import org.mozilla.focus.download.DownloadInfoManager
import org.mozilla.focus.utils.Constants
import org.mozilla.focus.widget.BackKeyHandleable
import org.mozilla.focus.widget.ResizableKeyboardLayout.OnKeyboardVisibilityChangedListener
import org.mozilla.permissionhandler.PermissionHandler
import org.mozilla.rocket.chrome.BottomBarItemAdapter
import org.mozilla.rocket.chrome.ChromeViewModel
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.common.data.ContentTabTelemetryData
import org.mozilla.rocket.content.getViewModel
import org.mozilla.rocket.content.view.BottomBar
import org.mozilla.rocket.extension.nonNullObserve
import org.mozilla.rocket.extension.switchFrom
import org.mozilla.rocket.privately.PrivateTabViewProvider
import org.mozilla.rocket.tabs.SessionManager
import org.mozilla.rocket.tabs.TabViewProvider
import org.mozilla.rocket.tabs.TabsSessionProvider
import javax.inject.Inject

class ContentTabActivity : BaseActivity(), TabsSessionProvider.SessionHost, ContentTabViewContract {

    @Inject
    lateinit var chromeViewModelCreator: Lazy<ChromeViewModel>

    @Inject
    lateinit var bottomBarViewModelCreator: Lazy<ContentTabBottomBarViewModel>

    @Inject
    lateinit var telemetryViewModelCreator: Lazy<ContentTabTelemetryViewModel>

    private lateinit var permissionHandler: PermissionHandler
    private lateinit var chromeViewModel: ChromeViewModel
    private lateinit var telemetryViewModel: ContentTabTelemetryViewModel
    private lateinit var tabViewProvider: TabViewProvider
    private lateinit var sessionManager: SessionManager
    private lateinit var contentTabHelper: ContentTabHelper
    private lateinit var contentTabObserver: ContentTabHelper.Observer
    private lateinit var uiMessageReceiver: BroadcastReceiver
    private lateinit var bottomBarItemAdapter: BottomBarItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_content_tab)

        chromeViewModel = getViewModel(chromeViewModelCreator)
        telemetryViewModel = getViewModel(telemetryViewModelCreator)
        tabViewProvider = PrivateTabViewProvider(this)
        sessionManager = SessionManager(tabViewProvider)

        appbar.setOnApplyWindowInsetsListener { v, insets ->
            (v.layoutParams as ConstraintLayout.LayoutParams).topMargin = insets.systemWindowInsetTop
            insets
        }

        makeStatusBarTransparent()

        setupBottomBar(bottom_bar)

        initBroadcastReceivers()

        contentTabHelper = ContentTabHelper(this)
        contentTabHelper.initPermissionHandler()

        contentTabObserver = contentTabHelper.getObserver()
        sessionManager.register(contentTabObserver)

        observeChromeAction()
        chromeViewModel.showUrlInput.value = chromeViewModel.currentUrl.value

        telemetryViewModel.initialize(intent?.extras?.getParcelable(EXTRA_TELEMETRY_DATA))

        if (savedInstanceState == null) {
            val url = intent?.extras?.getString(EXTRA_URL) ?: ""
            val enableTurboMode = intent?.extras?.getBoolean(EXTRA_ENABLE_TURBO_MODE) ?: true
            val contentTabFragment = ContentTabFragment.newInstance(url, enableTurboMode)
            supportFragmentManager.beginTransaction()
                .replace(R.id.browser_container, contentTabFragment)
                .commit()

            Looper.myQueue().addIdleHandler {
                contentTabFragment.setOnKeyboardVisibilityChangedListener(OnKeyboardVisibilityChangedListener { visible ->
                    if (visible) {
                        contentTabFragment.setOnKeyboardVisibilityChangedListener(null)
                        telemetryViewModel.onKeyboardShown()
                    }
                })
                false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sessionManager.resume()
        val uiActionFilter = IntentFilter()
        uiActionFilter.addCategory(Constants.CATEGORY_FILE_OPERATION)
        uiActionFilter.addAction(Constants.ACTION_NOTIFY_RELOCATE_FINISH)
        LocalBroadcastManager.getInstance(this).registerReceiver(uiMessageReceiver, uiActionFilter)
        telemetryViewModel.onSessionStarted()
    }

    override fun onPause() {
        super.onPause()
        sessionManager.pause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(uiMessageReceiver)
        telemetryViewModel.onSessionEnded()
    }

    override fun onDestroy() {
        super.onDestroy()
        sessionManager.unregister(contentTabObserver)
        sessionManager.destroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            toolbar_root.visibility = View.GONE
            bottom_bar.visibility = View.GONE
        } else {
            toolbar_root.visibility = View.VISIBLE
            bottom_bar.visibility = View.VISIBLE
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        permissionHandler.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }

    override fun onBackPressed() {
        if (supportFragmentManager.isStateSaved) {
            return
        }

        val fragment = supportFragmentManager.findFragmentById(R.id.browser_container)
        if (fragment != null && fragment is BackKeyHandleable) {
            val handled = fragment.onBackPressed()
            if (handled) {
                return
            }
        }

        super.onBackPressed()
    }

    override fun applyLocale() = Unit

    override fun getSessionManager() = sessionManager

    override fun getHostActivity() = this

    override fun getCurrentSession() = sessionManager.focusSession

    override fun getChromeViewModel() = chromeViewModel

    override fun getSiteIdentity(): ImageView? = site_identity

    override fun getDisplayUrlView(): TextView? = display_url

    override fun getProgressBar(): ProgressBar? = progress

    override fun getFullScreenGoneViews() = listOf(toolbar_root, bottom_bar)

    override fun getFullScreenInvisibleViews() = listOf(browser_container)

    override fun getFullScreenContainerView(): ViewGroup = video_container

    private fun setupBottomBar(bottomBar: BottomBar) {
        bottomBar.setOnItemClickListener(object : BottomBar.OnItemClickListener {
            override fun onItemClick(type: Int, position: Int) {
                when (type) {
                    BottomBarItemAdapter.TYPE_BACK -> chromeViewModel.goBack.call()
                    BottomBarItemAdapter.TYPE_REFRESH -> {
                        chromeViewModel.refreshOrStop.call()
                        telemetryViewModel.onReloadButtonClicked()
                    }
                    BottomBarItemAdapter.TYPE_SHARE -> chromeViewModel.share.call()
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
            .observe(this, Observer {
                bottomBarItemAdapter.setRefreshing(it == true)

                if (it == true) {
                    telemetryViewModel.onPageLoadingStarted()
                } else {
                    telemetryViewModel.onPageLoadingStopped()
                }
            })
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
                    DownloadInfoManager.getInstance().showOpenDownloadSnackBar(intent.getLongExtra(Constants.EXTRA_ROW_ID, -1), snack_bar_container, LOG_TAG)
                }
            }
        }
    }

    private fun observeChromeAction() {
        chromeViewModel.goBack.observe(this, Observer {
            onBackPressed()
            telemetryViewModel.onBackButtonClicked()
        })

        chromeViewModel.share.observe(this, Observer {
            chromeViewModel.currentUrl.value?.let { url ->
                onShareClicked(url)
            }
            telemetryViewModel.onShareButtonClicked()
        })

        chromeViewModel.currentUrl.observe(this, Observer {
            telemetryViewModel.onUrlOpened()
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
        private const val EXTRA_TELEMETRY_DATA = "telemetry_data"
        private const val EXTRA_ENABLE_TURBO_MODE = "enable_turbo_mode"

        fun getStartIntent(
            context: Context,
            url: String,
            telemetryData: ContentTabTelemetryData? = null,
            enableTurboMode: Boolean = true
        ) =
            Intent(context, ContentTabActivity::class.java).also { intent ->
                intent.putExtra(EXTRA_URL, url)
                intent.putExtra(EXTRA_ENABLE_TURBO_MODE, enableTurboMode)
                telemetryData?.let { intent.putExtra(EXTRA_TELEMETRY_DATA, it) }
            }
    }
}
