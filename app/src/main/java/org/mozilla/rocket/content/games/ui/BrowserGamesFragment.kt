package org.mozilla.rocket.content.games.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_games.recycler_view
import kotlinx.android.synthetic.main.fragment_games.spinner
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.activityViewModelProvider
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.games.ui.adapter.CarouselBanner
import org.mozilla.rocket.content.games.ui.adapter.CarouselBannerAdapterDelegate
import org.mozilla.rocket.content.games.ui.adapter.GameCategory
import org.mozilla.rocket.content.games.ui.adapter.GameCategoryAdapterDelegate
import javax.inject.Inject

class BrowserGamesFragment : Fragment() {

    @Inject
    lateinit var gamesViewModelFactory: GamesViewModelFactory

    private lateinit var gamesViewModel: GamesViewModel
    private lateinit var adapter: DelegateAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        gamesViewModel = activityViewModelProvider(gamesViewModelFactory)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_games, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        bindListData()
        bindPageState()
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
        gamesViewModel.browserGamesItems.observe(this@BrowserGamesFragment, Observer {
            adapter.setData(it)
        })
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
}