package org.mozilla.rocket.content.game.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import dagger.Lazy
import kotlinx.android.synthetic.main.activity_game_mode.*
import org.mozilla.focus.R
import org.mozilla.focus.activity.BaseActivity
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.widget.ResizableKeyboardLayout.OnKeyboardVisibilityChangedListener
import org.mozilla.rocket.chrome.ChromeViewModel
import org.mozilla.rocket.component.LaunchIntentDispatcher
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.common.data.ContentTabTelemetryData
import org.mozilla.rocket.content.common.ui.ContentTabFragment
import org.mozilla.rocket.content.common.ui.ContentTabHelper
import org.mozilla.rocket.content.common.ui.ContentTabTelemetryViewModel
import org.mozilla.rocket.content.common.ui.ContentTabViewContract
import org.mozilla.rocket.content.getViewModel
import org.mozilla.rocket.privately.PrivateTabViewProvider
import org.mozilla.rocket.tabs.Session
import org.mozilla.rocket.tabs.SessionManager
import org.mozilla.rocket.tabs.TabViewProvider
import org.mozilla.rocket.tabs.TabsSessionProvider
import javax.inject.Inject

class GameModeActivity : BaseActivity(), TabsSessionProvider.SessionHost, ContentTabViewContract {

    @Inject
    lateinit var chromeViewModelCreator: Lazy<ChromeViewModel>

    @Inject
    lateinit var telemetryViewModelCreator: Lazy<ContentTabTelemetryViewModel>

    private lateinit var chromeViewModel: ChromeViewModel
    private lateinit var telemetryViewModel: ContentTabTelemetryViewModel
    private lateinit var tabViewProvider: TabViewProvider
    private lateinit var sessionManager: SessionManager
    private lateinit var contentTabHelper: ContentTabHelper
    private lateinit var contentTabObserver: ContentTabHelper.Observer

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_mode)

        chromeViewModel = getViewModel(chromeViewModelCreator)
        telemetryViewModel = getViewModel(telemetryViewModelCreator)
        tabViewProvider = PrivateTabViewProvider(this)
        sessionManager = SessionManager(tabViewProvider)

        contentTabHelper = ContentTabHelper(this)
        contentTabHelper.initPermissionHandler()

        contentTabObserver = contentTabHelper.getObserver()
        sessionManager.register(contentTabObserver)

        observeChromeAction()
        chromeViewModel.showUrlInput.value = chromeViewModel.currentUrl.value

        telemetryViewModel.initialize(intent?.extras?.getParcelable(EXTRA_TELEMETRY_DATA))

        if (savedInstanceState == null) {
            val url = intent?.extras?.getString(EXTRA_URL) ?: ""
            val contentTabFragment = ContentTabFragment.newInstance(url, enableTurboMode = false, forceDisableImageBlocking = true)
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, contentTabFragment)
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

            if (isIntentFromGameShortcut(intent)) {
                TelemetryWrapper.launchByGameShortcut()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sessionManager.resume()
        telemetryViewModel.onSessionStarted()
    }

    override fun onPause() {
        super.onPause()
        sessionManager.pause()
        telemetryViewModel.onSessionEnded()
    }

    override fun onDestroy() {
        super.onDestroy()
        sessionManager.unregister(contentTabObserver)
        sessionManager.destroy()
    }

    override fun applyLocale() = Unit

    override fun getSessionManager(): SessionManager = sessionManager

    override fun getHostActivity(): AppCompatActivity = this

    override fun getCurrentSession(): Session? = sessionManager.focusSession

    override fun getChromeViewModel(): ChromeViewModel = chromeViewModel

    override fun getSiteIdentity(): ImageView? = null

    override fun getDisplayUrlView(): TextView? = null

    override fun getProgressBar(): ProgressBar? = null

    override fun getFullScreenGoneViews(): List<View> = emptyList()

    override fun getFullScreenInvisibleViews(): List<View> = listOf(fragment_container)

    override fun getFullScreenContainerView(): ViewGroup = video_container

    private fun observeChromeAction() {
        chromeViewModel.isRefreshing.observe(this, Observer {
            if (it == true) {
                telemetryViewModel.onPageLoadingStarted()
            } else {
                telemetryViewModel.onPageLoadingStopped()
            }
        })

        chromeViewModel.currentUrl.observe(this, Observer {
            telemetryViewModel.onUrlOpened()
        })
    }

    private fun isIntentFromGameShortcut(intent: Intent): Boolean {
        return intent.getBooleanExtra(
            LaunchIntentDispatcher.LaunchMethod.EXTRA_BOOL_GAME_SHORTCUT.value,
            false
        )
    }

    companion object {
        private const val EXTRA_URL = "url"
        private const val EXTRA_TELEMETRY_DATA = "telemetry_data"

        fun getStartIntent(
            context: Context,
            url: String,
            telemetryData: ContentTabTelemetryData? = null
        ) =
            Intent(context, GameModeActivity::class.java).also { intent ->
                intent.putExtra(EXTRA_URL, url)
                telemetryData?.let { intent.putExtra(EXTRA_TELEMETRY_DATA, it) }
            }
    }
}