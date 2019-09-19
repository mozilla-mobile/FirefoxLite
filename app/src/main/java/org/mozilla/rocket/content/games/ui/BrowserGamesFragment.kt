package org.mozilla.rocket.content.games.ui

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.Lazy
import kotlinx.android.synthetic.main.fragment_games.recycler_view
import kotlinx.android.synthetic.main.fragment_games.spinner
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.games.ui.adapter.CarouselBanner
import org.mozilla.rocket.content.games.ui.adapter.CarouselBannerAdapterDelegate
import org.mozilla.rocket.content.games.ui.adapter.GameCategoryAdapterDelegate
import org.mozilla.rocket.content.getViewModel
import javax.inject.Inject
import org.mozilla.rocket.content.appContext
import org.mozilla.rocket.content.games.vo.GameCategory
import java.util.Arrays
import android.webkit.URLUtil
import androidx.core.content.ContextCompat
import org.mozilla.focus.download.EnqueueDownloadTask
import org.mozilla.focus.web.WebViewProvider
import org.mozilla.rocket.content.common.ui.ContentTabActivity
import org.mozilla.rocket.tabs.web.Download
import java.net.HttpURLConnection
import java.net.URL

class BrowserGamesFragment : Fragment() {

    @Inject
    lateinit var gamesViewModelCreator: Lazy<GamesViewModel>

    private lateinit var gamesViewModel: GamesViewModel
    private lateinit var adapter: DelegateAdapter
    private lateinit var gameType: GameType

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        gamesViewModel = getViewModel(gamesViewModelCreator)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_games, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        bindListData()
        bindPageState()
        registerForContextMenu(recycler_view)
        observeGameAction()
    }

    fun createShortcut(gameName: String, gameURL: String, gameIcon: Bitmap) {
        val i = Intent(appContext(),
                GameModeActivity::class.java)
        i.action = Intent.ACTION_MAIN
        i.data = Uri.parse(gameURL)
        i.putExtra(GAME_URL, gameURL)

        if (Build.VERSION.SDK_INT < 26) {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val installer = Intent()
            installer.putExtra("android.intent.extra.shortcut.INTENT", i)
            installer.putExtra("android.intent.extra.shortcut.NAME", gameName)
            installer.setAction("com.android.launcher.action.INSTALL_SHORTCUT")
            installer.putExtra("duplicate", false)
            installer.putExtra("android.intent.extra.shortcut.ICON", gameIcon)
            appContext().sendBroadcast(installer)
        } else {
            val shortcutManager = activity?.getSystemService(ShortcutManager::class.java)

            if (shortcutManager!!.isRequestPinShortcutSupported) {
                val shortcut = ShortcutInfo.Builder(context, gameName)
                        .setShortLabel(gameName)
                        .setIcon(Icon.createWithAdaptiveBitmap(gameIcon))
                        .setIntent(i).build()

                shortcutManager.setDynamicShortcuts(Arrays.asList(shortcut))
                if (shortcutManager.isRequestPinShortcutSupported) {

                    val pinnedShortcutCallbackIntent = shortcutManager.createShortcutResultIntent(shortcut)
                    val successCallback = PendingIntent.getBroadcast(activity, 0,
                            pinnedShortcutCallbackIntent, 0)
                    shortcutManager.requestPinShortcut(shortcut, successCallback.intentSender)
                }
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val intent = item.getIntent()
        var gameType = ""
        if (intent != null) {
            gameType = intent.getStringExtra("gameType")
        }
        if ((gameType == "Browser" && this.gameType == GameType.TYPE_BROWSER) ||
                (gameType == "Premium" && this.gameType == GameType.TYPE_PREMIUM)) {
            when (item.getItemId()) {
                R.id.share -> {
                    val sendIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_SUBJECT, gamesViewModel.selectedGame.name)
                        putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.gaming_vertical_share_message, gamesViewModel.selectedGame.linkUrl))
                        type = "text/plain"
                    }
                    startActivity(Intent.createChooser(sendIntent, null))
                }
                R.id.remove -> {
                    gamesViewModel.removeGameFromRecentPlayList(gamesViewModel.selectedGame)
                }
                R.id.shortcut -> {
                    gamesViewModel.createShortCut()
                }
            }
            return true
        }
        return false
    }

    private fun initRecyclerView() {
        adapter = DelegateAdapter(
            AdapterDelegatesManager().apply {
                add(CarouselBanner::class, R.layout.item_carousel_banner, CarouselBannerAdapterDelegate(gamesViewModel))
                add(GameCategory::class, R.layout.item_game_category, GameCategoryAdapterDelegate(gamesViewModel))
            }
        )
        recycler_view.apply {
            adapter = this@BrowserGamesFragment.adapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun bindListData() {
        arguments?.getString(GAME_TYPE)?.let {
            gameType = GameType.valueOf(it)
        }

        when (gameType) {
            GameType.TYPE_BROWSER -> gamesViewModel.browserGamesItems.observe(this@BrowserGamesFragment, Observer {
                adapter.setData(it)
            })
            GameType.TYPE_PREMIUM -> gamesViewModel.premiumGamesItems.observe(this@BrowserGamesFragment, Observer {
                adapter.setData(it)
            })
        }
    }

    private fun bindPageState() {
        gamesViewModel.browserGamesState.observe(this@BrowserGamesFragment, Observer { state ->
            when (state) {
                is GamesViewModel.State.Idle -> showContentView()
                is GamesViewModel.State.Loading -> showLoadingView()
                is GamesViewModel.State.Error -> showErrorView()
            }
        })
    }

    private fun queueDownload(download: Download?, url: String) {
        val activity = activity
        if (activity == null || download == null) {
            return
        }

        EnqueueDownloadTask(getActivity()!!, download, url).execute()
    }

    fun downloadPremiumGame(downloadURL: String) {
        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(context!!, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            // We do have the permission to write to the external storage. Proceed with the download.
            var url = downloadURL
            var contentDisposition = ""
            var mimetype = "application/vnd.android.package-archive"
            var name = URLUtil.guessFileName(url, contentDisposition, mimetype)
            var download = Download(url,
                    name,
                    WebViewProvider.getUserAgentString(context),
                    contentDisposition,
                    mimetype, 0, false)

            queueDownload(download, url)
        }
    }

    private fun observeGameAction() {
        gamesViewModel.createShortcutEvent.observe(this, Observer { event ->
            val gameShortcut = event
            createShortcut(gameShortcut.gameName, gameShortcut.gameUrl, gameShortcut.gameBitmap)
        })

        gamesViewModel.event.observe(this, Observer { event ->
            when (event) {
                is GamesViewModel.GameAction.Play -> {
                    val play: GamesViewModel.GameAction.Play = event
                    startActivity(GameModeActivity.getStartIntent(context!!, play.url))
                }

                is GamesViewModel.GameAction.OpenLink -> {
                    val openLink: GamesViewModel.GameAction.OpenLink = event
                    startActivity(ContentTabActivity.getStartIntent(context!!, openLink.url))
                }

                is GamesViewModel.GameAction.Install -> {

                    // val install: GamesViewModel.GameAction.Install = event
                    // installa a APK
                    val install: GamesViewModel.GameAction.Install = event
                    downloadPremiumGame(install.url)
                }
            }
        })
    }

    private fun showLoadingView() {
        spinner.visibility = View.VISIBLE
    }

    private fun showContentView() {
        spinner.visibility = View.GONE
    }

    private fun showErrorView() {
        TODO("not implemented")
    }

    fun getFinalURL(url: String): String {

        try {
            var con = URL(url).openConnection() as HttpURLConnection
            con.setInstanceFollowRedirects(false)
            con.connect()
            con.getInputStream()

            if (con.getResponseCode() == HttpURLConnection.HTTP_MOVED_PERM || con.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) {
                var redirectUrl = con.getHeaderField("Location")
                return getFinalURL(redirectUrl)
            }
        } catch (e: Exception) {
            TODO("not implemented")
        }
            return url
        }

    companion object {

        private const val GAME_TYPE = "game_type"
        private const val GAME_URL = "url"
        enum class GameType {
            TYPE_BROWSER,
            TYPE_PREMIUM
        }
        @JvmStatic
        fun newInstance(gameType: GameType) = BrowserGamesFragment().apply {
            arguments = Bundle().apply {
                this.putString(GAME_TYPE, gameType.name)
            }
        }
    }
}