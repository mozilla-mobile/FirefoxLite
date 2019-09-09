package org.mozilla.rocket.content.games.ui

import android.content.Intent
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
import org.mozilla.rocket.content.games.ui.adapter.GameCategory
import org.mozilla.rocket.content.games.ui.adapter.GameCategoryAdapterDelegate
import org.mozilla.rocket.content.getViewModel
import javax.inject.Inject
import android.widget.Toast

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
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {
            R.id.share -> {
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_SUBJECT, gamesViewModel.selectedGame.name)
                    putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.share_game_dialog_text, gamesViewModel.selectedGame.linkUrl))
                    type = "text/plain"
                }
                startActivity(Intent.createChooser(sendIntent, null))
                Toast.makeText(activity, "Share", Toast.LENGTH_LONG).show()
            }
            R.id.remove -> Toast.makeText(activity, "Remove", Toast.LENGTH_LONG).show()
            R.id.shortcut -> Toast.makeText(activity, "Shortcut", Toast.LENGTH_LONG).show()
            R.id.delete -> Toast.makeText(activity, "Uninstall", Toast.LENGTH_LONG).show()
        }
        return super.onContextItemSelected(item)
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
            GameType.TYPE_PREMIUM -> gamesViewModel.browserGamesItems.observe(this@BrowserGamesFragment, Observer {
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

        private val GAME_TYPE = "game_type"

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