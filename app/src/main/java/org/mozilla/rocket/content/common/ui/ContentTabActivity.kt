package org.mozilla.rocket.content.common.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import dagger.Lazy
import org.mozilla.focus.R
import org.mozilla.focus.activity.BaseActivity
import org.mozilla.focus.download.DownloadInfoManager
import org.mozilla.focus.utils.Constants
import org.mozilla.focus.utils.IntentUtils
import org.mozilla.focus.widget.BackKeyHandleable
import org.mozilla.rocket.chrome.BottomBarItemAdapter
import org.mozilla.rocket.chrome.ChromeViewModel
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.getViewModel
import org.mozilla.rocket.content.view.BottomBar
import org.mozilla.rocket.extension.nonNullObserve
import org.mozilla.rocket.extension.switchFrom
import org.mozilla.rocket.privately.PrivateTabViewProvider
import org.mozilla.rocket.tabs.SessionManager
import org.mozilla.rocket.tabs.TabsSessionProvider
import javax.inject.Inject

class ContentTabActivity : BaseActivity(), TabsSessionProvider.SessionHost {

    @Inject
    lateinit var chromeViewModelCreator: Lazy<ChromeViewModel>

    @Inject
    lateinit var bottomBarViewModelCreator: Lazy<ContentTabBottomBarViewModel>

    private var sessionManager: SessionManager? = null
    private lateinit var chromeViewModel: ChromeViewModel
    private lateinit var tabViewProvider: PrivateTabViewProvider
    private lateinit var uiMessageReceiver: BroadcastReceiver
    private lateinit var bottomBarItemAdapter: BottomBarItemAdapter
    private lateinit var bottomBar: BottomBar
    private lateinit var snackBarContainer: View

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_content_tab)

        chromeViewModel = getViewModel(chromeViewModelCreator)
        tabViewProvider = PrivateTabViewProvider(this)

        snackBarContainer = findViewById(R.id.snack_bar_container)
        makeStatusBarTransparent()
        bottomBar = findViewById(R.id.bottom_bar)
        setupBottomBar(bottomBar)

        initBroadcastReceivers()

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
        if (sessionManager == null) {
            sessionManager = SessionManager(tabViewProvider)
        }

        // we just created it, it definitely not null
        return sessionManager!!
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
}
