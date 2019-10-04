package org.mozilla.rocket.content.games.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import org.mozilla.focus.R
import org.mozilla.rocket.content.games.ui.BrowserGamesFragment

@Suppress("DEPRECATION")
class GameTabsAdapter(
    fm: FragmentManager,
    activity: FragmentActivity,
    private val items: List<TabItem> = getDefaultTabs()
) : FragmentPagerAdapter(fm) {

    private val resource = activity.resources

    override fun getItem(position: Int): Fragment = items[position].fragment

    override fun getCount(): Int = items.size

    override fun getPageTitle(position: Int): CharSequence? = resource.getString(items[position].titleResId)

    data class TabItem(
        val fragment: Fragment,
        val titleResId: Int
    )

    companion object {
        private fun getDefaultTabs(): List<TabItem> = listOf(
            TabItem(BrowserGamesFragment.newInstance(GameType.INSTANT), R.string.gaming_vertical_category_1),
            TabItem(BrowserGamesFragment.newInstance(GameType.DOWNLOAD), R.string.gaming_vertical_category_2)
        )
    }
}