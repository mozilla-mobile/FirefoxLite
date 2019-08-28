package org.mozilla.rocket.content.games.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import org.mozilla.rocket.content.games.ui.BrowserGamesFragment
import org.mozilla.rocket.content.games.ui.GamesActivity

@Suppress("DEPRECATION")
class GameTabsAdapter(
    fm: FragmentManager,
    activity: FragmentActivity,
    private val items: List<TabItem> = getDefaultTabs()
) : FragmentPagerAdapter(fm) {

    private val resource = activity.resources

    override fun getItem(position: Int): Fragment = items[position].fragment

    override fun getCount(): Int = items.size

    override fun getPageTitle(position: Int): CharSequence? = items[position].title

    data class TabItem(
        val fragment: Fragment,
        val title: String // TODO: change to 'titleResId'
    )

    companion object {
        private fun getDefaultTabs(): List<TabItem> = listOf(
            TabItem(BrowserGamesFragment(), "Browser Games"),
            TabItem(GamesActivity.PremiumGamesFragment(), "Premium Games")
        )
    }
}