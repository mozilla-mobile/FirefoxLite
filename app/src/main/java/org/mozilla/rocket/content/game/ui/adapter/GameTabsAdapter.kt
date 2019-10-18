package org.mozilla.rocket.content.game.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import org.mozilla.focus.BuildConfig
import org.mozilla.focus.R
import org.mozilla.rocket.content.game.ui.DownloadGameFragment
import org.mozilla.rocket.content.game.ui.InstantGameFragment

@Suppress("DEPRECATION")
class GameTabsAdapter(
    fm: FragmentManager,
    activity: FragmentActivity,
    private val items: List<TabItem> = getDefaultTabs()
) : FragmentStatePagerAdapter(fm) {

    private val resource = activity.resources

    override fun getItem(position: Int): Fragment = items[position].createFragment()

    override fun getCount(): Int = items.size

    override fun getPageTitle(position: Int): CharSequence? = resource.getString(items[position].titleResId)

    data class TabItem(
        val type: Int,
        val titleResId: Int
    ) {
        fun createFragment(): Fragment {
            return when (type) {
                TYPE_INSTANT_GAME_TAB -> InstantGameFragment.newInstance()
                TYPE_DOWNLOAD_GAME_TAB -> DownloadGameFragment.newInstance()
                else -> error("Unsupported game tab item type $type")
            }
        }

        companion object {
            const val TYPE_INSTANT_GAME_TAB = 0
            const val TYPE_DOWNLOAD_GAME_TAB = 1
        }
    }

    companion object {
        private fun getDefaultTabs(): List<TabItem> = if (BuildConfig.DEBUG) {
            //  Still enable download game tab in debug build for upcoming implementation
            listOf(
                TabItem(TabItem.TYPE_INSTANT_GAME_TAB, R.string.gaming_vertical_category_1),
                TabItem(TabItem.TYPE_DOWNLOAD_GAME_TAB, R.string.gaming_vertical_category_2)
            )
        } else {
            listOf(
                TabItem(TabItem.TYPE_INSTANT_GAME_TAB, R.string.gaming_vertical_category_1)
            )
        }
    }
}