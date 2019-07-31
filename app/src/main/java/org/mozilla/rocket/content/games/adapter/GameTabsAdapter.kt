package org.mozilla.rocket.content.games.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import org.mozilla.focus.R
import org.mozilla.rocket.content.games.BrowserGamesFragment
import org.mozilla.rocket.content.games.GamesActivity

class GameTabsAdapter(fm: FragmentManager, activity: FragmentActivity) : FragmentPagerAdapter(fm) {

    private val resource = activity.resources

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> BrowserGamesFragment()
            1 -> GamesActivity.PremiumGamesFragment()
            else -> throw IndexOutOfBoundsException("position: $position")
        }
    }

    override fun getCount(): Int = GAMES_TAB_COUNT

    override fun getPageTitle(position: Int): CharSequence? {
        val resId = when (position) {
            0 -> R.string.games_tab_title_browser_games
            1 -> R.string.games_tab_title_premium_games
            else -> throw IndexOutOfBoundsException("position: $position")
        }

        return resource.getString(resId)
    }

    companion object {
        const val GAMES_TAB_COUNT = 2
    }
}