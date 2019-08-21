package org.mozilla.rocket.content.common.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.mozilla.focus.BuildConfig
import org.mozilla.focus.R
import org.mozilla.focus.activity.BaseActivity
import org.mozilla.focus.download.DownloadInfoManager
import org.mozilla.focus.navigation.ScreenNavigator
import org.mozilla.focus.urlinput.UrlInputFragment
import org.mozilla.focus.utils.Constants
import org.mozilla.focus.utils.SafeIntent
import org.mozilla.rocket.chrome.ChromeViewModel
import org.mozilla.rocket.chrome.ChromeViewModelFactory
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.viewModelProvider
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
    lateinit var chromeViewModelFactory: ChromeViewModelFactory

    private var sessionManager: SessionManager? = null
    private val portraitStateModel = PortraitStateModel()
    private lateinit var chromeViewModel: ChromeViewModel
    private lateinit var tabViewProvider: PrivateTabViewProvider
    private lateinit var screenNavigator: ScreenNavigator
    private lateinit var uiMessageReceiver: BroadcastReceiver
    private lateinit var snackBarContainer: View

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)

        chromeViewModel = viewModelProvider(chromeViewModelFactory)
        tabViewProvider = PrivateTabViewProvider(this)
        screenNavigator = ScreenNavigator(this)

        handleIntent(intent)

        setContentView(R.layout.activity_content_tab)
        snackBarContainer = findViewById(R.id.container)
        makeStatusBarTransparent()

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
