package org.mozilla.rocket.content.games.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import org.mozilla.focus.R
import org.mozilla.rocket.content.games.ui.DownloadGameFragment
import org.mozilla.rocket.content.games.ui.InstantGameFragment

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
            TabItem(InstantGameFragment.newInstance(), R.string.gaming_vertical_category_1),
            TabItem(DownloadGameFragment.newInstance(), R.string.gaming_vertical_category_2)
        )
    }
}