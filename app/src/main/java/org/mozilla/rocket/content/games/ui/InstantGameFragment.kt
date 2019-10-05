package org.mozilla.rocket.content.games.ui

import android.app.PendingIntent
import android.content.Intent
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
import kotlinx.android.synthetic.main.fragment_game.*
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.appContext
import org.mozilla.rocket.content.common.adapter.Runway
import org.mozilla.rocket.content.common.adapter.RunwayAdapterDelegate
import org.mozilla.rocket.content.common.ui.ContentTabActivity
import org.mozilla.rocket.content.common.ui.RunwayViewModel
import org.mozilla.rocket.content.games.ui.adapter.GameCategory
import org.mozilla.rocket.content.games.ui.adapter.GameCategoryAdapterDelegate
import org.mozilla.rocket.content.games.ui.adapter.GameType
import org.mozilla.rocket.content.getActivityViewModel
import javax.inject.Inject

class InstantGameFragment : Fragment() {

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
        return inflater.inflate(R.layout.fragment_game, container, false)
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
            adapter = this@InstantGameFragment.adapter
        }
        registerForContextMenu(recycler_view)
    }

    private fun bindListData() {
        gamesViewModel.instantGameItems.observe(this@InstantGameFragment, Observer {
            adapter.setData(it)
        })
    }

    private fun bindPageState() {
        gamesViewModel.isDataLoading.observe(this@InstantGameFragment, Observer { state ->
            when (state) {
                is GamesViewModel.State.Idle -> showContentView()
                is GamesViewModel.State.Loading -> showLoadingView()
                is GamesViewModel.State.Error -> showErrorView()
            }
        })
    }

    private fun observeGameAction() {
        gamesViewModel.createShortcutEvent.observe(this, Observer { gameShortcut ->
            createShortcut(gameShortcut.gameName, gameShortcut.gameUrl, gameShortcut.gameBitmap)
        })

        gamesViewModel.event.observe(this, Observer { event ->
            when (event) {
                is GamesViewModel.GameAction.Play -> {
                    val play: GamesViewModel.GameAction.Play = event
                    context?.let {
                        startActivity(GameModeActivity.getStartIntent(it, play.url))
                    }
                }
                is GamesViewModel.GameAction.OpenLink -> {
                    val openLink: GamesViewModel.GameAction.OpenLink = event
                    context?.let {
                        startActivity(ContentTabActivity.getStartIntent(it, openLink.url))
                    }
                }
            }
        })
    }

    private fun createShortcut(gameName: String, gameURL: String, gameIcon: Bitmap) {
        val intent = Intent(appContext(), GameModeActivity::class.java)
        intent.action = Intent.ACTION_MAIN
        intent.data = Uri.parse(gameURL)
        intent.putExtra(GAME_URL, gameURL)

        if (Build.VERSION.SDK_INT < 26) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val installer = Intent()
            installer.putExtra("android.intent.extra.shortcut.INTENT", intent)
            installer.putExtra("android.intent.extra.shortcut.NAME", gameName)
            installer.action = "com.android.launcher.action.INSTALL_SHORTCUT"
            installer.putExtra("duplicate", false)
            installer.putExtra("android.intent.extra.shortcut.ICON", gameIcon)
            appContext().sendBroadcast(installer)
        } else {
            val shortcutManager = activity?.getSystemService(ShortcutManager::class.java)
            if (shortcutManager?.isRequestPinShortcutSupported == true) {
                val shortcut = ShortcutInfo.Builder(context, gameName)
                    .setShortLabel(gameName)
                    .setIcon(Icon.createWithAdaptiveBitmap(gameIcon))
                    .setIntent(intent).build()

                shortcutManager.dynamicShortcuts = listOf(shortcut)
                if (shortcutManager.isRequestPinShortcutSupported) {
                    val pinnedShortcutCallbackIntent = shortcutManager.createShortcutResultIntent(shortcut)
                    val successCallback = PendingIntent.getBroadcast(
                        activity,
                        0,
                        pinnedShortcutCallbackIntent,
                        0
                    )
                    shortcutManager.requestPinShortcut(shortcut, successCallback.intentSender)
                }
            }
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
        private const val GAME_URL = "url"

        fun newInstance() = InstantGameFragment()
    }
}