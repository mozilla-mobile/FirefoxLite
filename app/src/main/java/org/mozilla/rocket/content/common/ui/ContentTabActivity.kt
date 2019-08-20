package org.mozilla.rocket.content.common.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import dagger.Lazy
import org.mozilla.focus.BuildConfig
import org.mozilla.focus.R
import org.mozilla.focus.activity.BaseActivity
import org.mozilla.focus.download.DownloadInfoManager
import org.mozilla.focus.navigation.ScreenNavigator
import org.mozilla.focus.urlinput.UrlInputFragment
import org.mozilla.focus.utils.Constants
import org.mozilla.focus.utils.IntentUtils
import org.mozilla.focus.utils.SafeIntent
import org.mozilla.rocket.chrome.BottomBarItemAdapter
import org.mozilla.rocket.chrome.ChromeViewModel
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.getViewModel
import org.mozilla.rocket.content.view.BottomBar
import org.mozilla.rocket.extension.nonNullObserve
import org.mozilla.rocket.extension.switchFrom
import org.mozilla.rocket.landing.NavigationModel
import org.mozilla.rocket.landing.OrientationState
import org.mozilla.rocket.landing.PortraitStateModel
import org.mozilla.rocket.privately.PrivateTabViewProvider
import org.mozilla.rocket.privately.home.PrivateHomeFragment
import org.mozilla.rocket.tabs.SessionManager
import org.mozilla.rocket.tabs.TabsSessionProvider
import javax.inject.Inject

class ContentTabActivity : BaseActivity(),
    ScreenNavigator.Provider,
    ScreenNavigator.HostActivity,
    TabsSessionProvider.SessionHost {

    @Inject
    lateinit var chromeViewModelCreator: Lazy<ChromeViewModel>

    @Inject
    lateinit var bottomBarViewModelCreator: Lazy<ContentTabBottomBarViewModel>

    private var sessionManager: SessionManager? = null
    private val portraitStateModel = PortraitStateModel()
    private lateinit var chromeViewModel: ChromeViewModel
    private lateinit var tabViewProvider: PrivateTabViewProvider
    private lateinit var screenNavigator: ScreenNavigator
    private lateinit var uiMessageReceiver: BroadcastReceiver
    private lateinit var bottomBarItemAdapter: BottomBarItemAdapter
    private lateinit var bottomBar: BottomBar
    private lateinit var snackBarContainer: View

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)

        chromeViewModel = getViewModel(chromeViewModelCreator)
        tabViewProvider = PrivateTabViewProvider(this)
        screenNavigator = ScreenNavigator(this)

        handleIntent(intent)

        setContentView(R.layout.activity_content_tab)
        snackBarContainer = findViewById(R.id.container)
        makeStatusBarTransparent()
        bottomBar = findViewById(R.id.bottom_bar)
        setupBottomBar(bottomBar)

        initBroadcastReceivers()

        observeChromeAction()
        chromeViewModel.showUrlInput.value = chromeViewModel.currentUrl.value

        monitorOrientationState()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        handleIntent(intent)
        setIntent(intent)
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
        sessionManager?.destroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            bottomBar.visibility = View.GONE
        } else {
            bottomBar.visibility = View.VISIBLE
        }
    }

    override fun applyLocale() {}

    override fun onBackPressed() {
        if (supportFragmentManager.isStateSaved) {
            return
        }

        val handled = screenNavigator.visibleBrowserScreen?.onBackPressed() ?: false
        if (handled) {
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

    override fun getScreenNavigator(): ScreenNavigator = screenNavigator

    override fun getBrowserScreen(): ScreenNavigator.BrowserScreen {
        return supportFragmentManager.findFragmentById(R.id.browser) as ContentTabFragment
    }

    override fun createFirstRunScreen(): ScreenNavigator.Screen {
        if (BuildConfig.DEBUG) {
            throw RuntimeException("ContentTabActivity should never show first-run")
        }
        TODO("ContentTabActivity should never show first-run")
    }

    override fun createHomeScreen(): ScreenNavigator.HomeScreen {
        return PrivateHomeFragment.create()
    }

    override fun createUrlInputScreen(url: String?, parentFragmentTag: String?): ScreenNavigator.UrlInputScreen {
        return UrlInputFragment.create(url, null, false)
    }

    override fun createMissionDetailScreen(): ScreenNavigator.MissionDetailScreen {
        if (BuildConfig.DEBUG) {
            throw RuntimeException("ContentTabActivity should never show Mission Detail")
        }
        TODO("ContentTabActivity should never show Mission Detail")
    }

    override fun createFxLoginScreen(): ScreenNavigator.FxLoginScreen {
        if (BuildConfig.DEBUG) {
            throw RuntimeException("ContentTabActivity should never show FxLogin")
        }
        TODO("ContentTabActivity should never show FxLogin")
    }

    override fun createRedeemScreen(): ScreenNavigator.RedeemSceen {
        if (BuildConfig.DEBUG) {
            throw RuntimeException("ContentTabActivity should never show Redeem")
        }
        TODO("ContentTabActivity should never show Redeem")
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

    private fun handleIntent(intent: Intent?) {
        val safeIntent = intent?.let { SafeIntent(it) } ?: return

        val fromHistory = (safeIntent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0
        if (!fromHistory) {
            safeIntent.getStringExtra(EXTRA_URL)?.let { url ->
                chromeViewModel.openUrl.value = ChromeViewModel.OpenUrlAction(url, withNewTab = false, isFromExternal = true)
            }
        }
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

    private fun observeChromeAction() {
        chromeViewModel.openUrl.observe(this, Observer { action ->
            action?.run {
                screenNavigator.showBrowserScreen(url, false, isFromExternal)
            }
        })
        chromeViewModel.showUrlInput.observe(this, Observer { url ->
            if (!supportFragmentManager.isStateSaved) {
                screenNavigator.addUrlScreen(url)
            }
        })
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

    private fun monitorOrientationState() {
        val orientationState = OrientationState(object : NavigationModel {
            override val navigationState: LiveData<ScreenNavigator.NavigationState>
                get() = ScreenNavigator.get(this@ContentTabActivity).navigationState
        }, portraitStateModel)

        orientationState.observe(this, Observer { orientation ->
            orientation?.let {
                requestedOrientation = it
            }
        })
    }

    companion object {
        private const val LOG_TAG = "ContentTabActivity"
        private const val EXTRA_URL = "url"

        fun getStartIntent(context: Context, url: String) = Intent(context, ContentTabActivity::class.java).also { it.putExtra(EXTRA_URL, url) }
    }
}
