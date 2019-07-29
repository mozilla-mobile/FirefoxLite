package org.mozilla.rocket.vertical.games

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_games.list
import org.mozilla.focus.R

class BrowserGamesFragment : Fragment() {

    private lateinit var adapter: BrowserGamesAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_games, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        bindListData()
    }

    private fun initRecyclerView() {
        adapter = BrowserGamesAdapter()
        list.apply {
            adapter = this@BrowserGamesFragment.adapter
        }
    }

    private fun bindListData() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}