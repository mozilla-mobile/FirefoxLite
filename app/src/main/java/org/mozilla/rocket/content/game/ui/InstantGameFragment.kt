package org.mozilla.rocket.content.game.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import dagger.Lazy
import kotlinx.android.synthetic.main.fragment_game.*
import org.mozilla.focus.R
import org.mozilla.focus.utils.ShortcutUtils
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.common.adapter.Runway
import org.mozilla.rocket.content.common.adapter.RunwayAdapterDelegate
import org.mozilla.rocket.content.common.ui.RunwayViewModel
import org.mozilla.rocket.content.game.ui.adapter.InstantGameCategoryAdapterDelegate
import org.mozilla.rocket.content.game.ui.model.GameCategory
import org.mozilla.rocket.content.getActivityViewModel
import javax.inject.Inject

class InstantGameFragment : Fragment() {

    @Inject
    lateinit var runwayViewModelCreator: Lazy<RunwayViewModel>

    @Inject
    lateinit var instantGameViewModelCreator: Lazy<InstantGameViewModel>

    private lateinit var runwayViewModel: RunwayViewModel
    private lateinit var instantGamesViewModel: InstantGameViewModel
    private lateinit var adapter: DelegateAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        runwayViewModel = getActivityViewModel(runwayViewModelCreator)
        instantGamesViewModel = getActivityViewModel(instantGameViewModelCreator)
        instantGamesViewModel.requestGameList()
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
        initNoResultView()
    }

    private fun initRecyclerView() {
        adapter = DelegateAdapter(
            AdapterDelegatesManager().apply {
                add(Runway::class, R.layout.item_runway_list, RunwayAdapterDelegate(runwayViewModel))
                add(GameCategory::class, R.layout.item_game_category, InstantGameCategoryAdapterDelegate(instantGamesViewModel))
            }
        )
        recycler_view.apply {
            adapter = this@InstantGameFragment.adapter
        }
        registerForContextMenu(recycler_view)
    }

    private fun bindListData() {
        instantGamesViewModel.instantGameItems.observe(this@InstantGameFragment, Observer {
            adapter.setData(it)
        })
    }

    private fun bindPageState() {
        instantGamesViewModel.isDataLoading.observe(this@InstantGameFragment, Observer { state ->
            when (state) {
                is InstantGameViewModel.State.Idle -> showContentView()
                is InstantGameViewModel.State.Loading -> showLoadingView()
                is InstantGameViewModel.State.Error -> showErrorView()
            }
        })
    }

    private fun observeGameAction() {
        runwayViewModel.openRunway.observe(this, Observer { linkUrl ->
            context?.let {
                startActivity(GameModeActivity.getStartIntent(it, linkUrl))
            }
        })

        instantGamesViewModel.event.observe(this, Observer { event ->
            when (event) {
                is InstantGameViewModel.GameAction.Play -> {
                    val play: InstantGameViewModel.GameAction.Play = event
                    context?.let {
                        startActivity(GameModeActivity.getStartIntent(it, play.url))
                    }
                }
                is InstantGameViewModel.GameAction.Share -> {
                    val share: InstantGameViewModel.GameAction.Share = event
                    context?.let {
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, String.format(getString(R.string.gaming_vertical_share_message), share.url))
                            type = "text/plain"
                        }
                        startActivity(Intent.createChooser(sendIntent, null))
                    }
                }
                is InstantGameViewModel.GameAction.CreateShortcut -> {
                    val shortcut: InstantGameViewModel.GameAction.CreateShortcut = event
                    context?.let {
                        createShortcut(it, shortcut.name, shortcut.url, shortcut.bitmap)
                    }
                }
            }
        })
    }

    private fun initNoResultView() {
        no_result_view.setButtonOnClickListener(View.OnClickListener {
            instantGamesViewModel.onRetryButtonClicked()
        })
    }

    private fun createShortcut(context: Context, gameName: String, gameURL: String, gameIcon: Bitmap) {
        val intent = Intent(context, GameModeActivity::class.java)
        intent.action = Intent.ACTION_MAIN
        intent.data = Uri.parse(gameURL)
        intent.putExtra(GAME_URL, gameURL)

        ShortcutUtils.requestPinShortcut(context, intent, gameName, gameURL, gameIcon)
    }

    private fun showLoadingView() {
        spinner.visibility = View.VISIBLE
        recycler_view.visibility = View.GONE
        no_result_view.visibility = View.GONE
    }

    private fun showContentView() {
        spinner.visibility = View.GONE
        recycler_view.visibility = View.VISIBLE
        no_result_view.visibility = View.GONE
    }

    private fun showErrorView() {
        spinner.visibility = View.GONE
        recycler_view.visibility = View.GONE
        no_result_view.visibility = View.VISIBLE
    }

    companion object {
        private const val GAME_URL = "url"

        fun newInstance() = InstantGameFragment()
    }
}