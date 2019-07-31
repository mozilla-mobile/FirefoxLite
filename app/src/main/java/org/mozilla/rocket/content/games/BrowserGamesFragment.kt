package org.mozilla.rocket.content.games

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_games.list
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter

class BrowserGamesFragment : Fragment() {

    private lateinit var adapter: DelegateAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_games, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
    }

    private fun initRecyclerView() {
        adapter = DelegateAdapter(
            AdapterDelegatesManager()
        )
        list.apply {
            adapter = this@BrowserGamesFragment.adapter
            layoutManager = LinearLayoutManager(context)
        }
    }
}