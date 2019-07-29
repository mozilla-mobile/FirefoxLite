package org.mozilla.rocket.vertical.games

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_games.list
import org.mozilla.focus.R

class BrowserGamesFragment : Fragment() {

    private lateinit var gamesViewModel: GamesViewModel
    private lateinit var adapter: BrowserGamesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gamesViewModel = ViewModelProviders.of(requireActivity(), GamesViewModelFactory.INSTANCE).get(GamesViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_games, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        bindListData()
    }

    private fun initRecyclerView() {
        adapter = BrowserGamesAdapter(gamesViewModel)
        list.apply {
            adapter = this@BrowserGamesFragment.adapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun bindListData() {
        with(gamesViewModel) {
            browserGamesItems.observe(this@BrowserGamesFragment, Observer {
                adapter.setData(it)
            })
        }
    }
}