package org.mozilla.rocket.vertical.games

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import kotlinx.android.synthetic.main.activity_games.view_pager
import org.mozilla.focus.R

class GamesActivity : FragmentActivity() {

    private lateinit var adapter: GamesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_games)
        initViewPager()
    }

    private fun initViewPager() {
        adapter = GamesAdapter(this)
        view_pager.apply {
            adapter = this@GamesActivity.adapter
        }
    }

    private class GamesAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

        override fun getItemCount(): Int = GAMES_TAB_COUNT

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> BrowserGamesFragment()
                1 -> PremiumGamesFragment()
                else -> throw IndexOutOfBoundsException("position: $position")
            }
        }
    }

    class PremiumGamesFragment : Fragment()

    companion object {
        const val GAMES_TAB_COUNT = 2

        fun getStartIntent(context: Context) = Intent(context, GamesActivity::class.java)
    }
}