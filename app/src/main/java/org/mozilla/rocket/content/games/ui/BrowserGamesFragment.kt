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
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import dagger.Lazy
import kotlinx.android.synthetic.main.fragment_games.recycler_view
import kotlinx.android.synthetic.main.fragment_games.spinner
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.games.ui.adapter.GameCategoryAdapterDelegate
import javax.inject.Inject
import org.mozilla.rocket.content.appContext
import java.util.Arrays
import android.webkit.URLUtil
import androidx.core.content.ContextCompat
import org.mozilla.focus.download.EnqueueDownloadTask
import org.mozilla.focus.web.WebViewProvider
import org.mozilla.rocket.content.common.adapter.Runway
import org.mozilla.rocket.content.common.adapter.RunwayAdapterDelegate
import org.mozilla.rocket.content.common.ui.ContentTabActivity
import org.mozilla.rocket.content.common.ui.RunwayViewModel
import org.mozilla.rocket.content.games.ui.adapter.GameCategory
import org.mozilla.rocket.content.games.ui.adapter.GameType
import org.mozilla.rocket.content.getActivityViewModel
import org.mozilla.rocket.tabs.web.Download

class BrowserGamesFragment : Fragment() {

    @Inject
    lateinit var runwayViewModelCreator: Lazy<RunwayViewModel>

    @Inject
    lateinit var gamesViewModelCreator: Lazy<GamesViewModel>

    private lateinit var runwayViewModel: RunwayViewModel
    private lateinit var gamesViewModel: GamesViewModel
    private lateinit var adapter: DelegateAdapter
    private lateinit var gameType: GameType

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        runwayViewModel = getActivityViewModel(runwayViewModelCreator)
        gamesViewModel = getActivityViewModel(gamesViewModelCreator)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_games, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        bindListData()
        bindPageState()
        observeGameAction()
    }

    private fun initRecyclerView() {
        adapter = DelegateAdapter(
            AdapterDelegatesManager().apply {
                add(Runway::class, R.layout.item_runway_list, RunwayAdapterDelegate(runwayViewModel))
                add(GameCategory::class, R.layout.item_game_category, GameCategoryAdapterDelegate(gamesViewModel))
            }
        )
        recycler_view.apply {
            adapter = this@BrowserGamesFragment.adapter
        }
        registerForContextMenu(recycler_view)
    }

    private fun bindListData() {
        arguments?.getString(GAME_TYPE)?.let {
            gameType = GameType.valueOf(it)
        }

        when (gameType) {
            GameType.INSTANT -> gamesViewModel.instantGameItems.observe(this@BrowserGamesFragment, Observer {
                adapter.setData(it)
            })
            GameType.DOWNLOAD -> gamesViewModel.downloadGameItems.observe(this@BrowserGamesFragment, Observer {
                adapter.setData(it)
            })
        }
    }

    private fun bindPageState() {
        gamesViewModel.isDataLoading.observe(this@BrowserGamesFragment, Observer { state ->
            when (state) {
                is GamesViewModel.State.Idle -> showContentView()
                is GamesViewModel.State.Loading -> showLoadingView()
                is GamesViewModel.State.Error -> showErrorView()
            }
        })
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
            installer.action = "com.android.launcher.action.INSTALL_SHORTCUT"
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

                shortcutManager.dynamicShortcuts = Arrays.asList(shortcut)
                if (shortcutManager.isRequestPinShortcutSupported) {

                    val pinnedShortcutCallbackIntent = shortcutManager.createShortcutResultIntent(shortcut)
                    val successCallback = PendingIntent.getBroadcast(activity, 0,
                            pinnedShortcutCallbackIntent, 0)
                    shortcutManager.requestPinShortcut(shortcut, successCallback.intentSender)
                }
            }
        }
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

    private fun showLoadingView() {
        spinner.visibility = View.VISIBLE
    }

    private fun showContentView() {
        spinner.visibility = View.GONE
    }

    private fun showErrorView() {
        TODO("not implemented")
    }

    companion object {
        private const val GAME_TYPE = "game_type"
        private const val GAME_URL = "url"

        @JvmStatic
        fun newInstance(gameType: GameType) = BrowserGamesFragment().apply {
            arguments = Bundle().apply {
                this.putString(GAME_TYPE, gameType.name)
            }
        }
    }
}