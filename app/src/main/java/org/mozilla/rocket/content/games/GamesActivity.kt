package org.mozilla.rocket.content.games

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import kotlinx.android.synthetic.main.activity_games.games_tabs
import kotlinx.android.synthetic.main.activity_games.view_pager
import org.mozilla.focus.R
import org.mozilla.rocket.content.games.adapter.GameTabsAdapter

class GamesActivity : FragmentActivity() {

    private lateinit var adapter: GameTabsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_games)
        initViewPager()
        initTabLayout()
    }

    private fun initViewPager() {
        adapter = GameTabsAdapter(supportFragmentManager, this)
        view_pager.apply {
            adapter = this@GamesActivity.adapter
        }
    }

    private fun initTabLayout() {
        games_tabs.setupWithViewPager(view_pager)
    }

    class PremiumGamesFragment : Fragment()

    companion object {
        fun getStartIntent(context: Context) = Intent(context, GamesActivity::class.java)
    }
}