package org.mozilla.rocket.content.games.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import dagger.Lazy
import kotlinx.android.synthetic.main.activity_game_mode.*
import org.mozilla.focus.R
import org.mozilla.focus.activity.BaseActivity
import org.mozilla.rocket.chrome.ChromeViewModel
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.common.ui.ContentTabFragment
import org.mozilla.rocket.content.common.ui.ContentTabHelper
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

    private lateinit var chromeViewModel: ChromeViewModel
    private lateinit var tabViewProvider: TabViewProvider
    private lateinit var sessionManager: SessionManager
    private lateinit var contentTabHelper: ContentTabHelper
    private lateinit var contentTabObserver: ContentTabHelper.Observer

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_mode)

        chromeViewModel = getViewModel(chromeViewModelCreator)
        tabViewProvider = PrivateTabViewProvider(this)
        sessionManager = SessionManager(tabViewProvider)

        contentTabHelper = ContentTabHelper(this)
        contentTabHelper.initPermissionHandler()

        contentTabObserver = contentTabHelper.getObserver()
        sessionManager.register(contentTabObserver)

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
        sessionManager.resume()
    }

    override fun onPause() {
        super.onPause()
        sessionManager.pause()
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

    companion object {
        private const val EXTRA_URL = "url"
        private const val EXTRA_ENABLE_TURBO_MODE = "enable_turbo_mode"

        fun getStartIntent(context: Context, url: String, enableTurboMode: Boolean = false) =
            Intent(context, GameModeActivity::class.java).also {
                it.putExtra(EXTRA_URL, url)
                it.putExtra(EXTRA_ENABLE_TURBO_MODE, enableTurboMode)
            }
    }
}