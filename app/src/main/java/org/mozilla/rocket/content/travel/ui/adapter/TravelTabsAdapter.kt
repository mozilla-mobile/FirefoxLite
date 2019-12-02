package org.mozilla.rocket.content.travel.ui.adapter

import android.os.Parcelable
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import kotlinx.android.parcel.Parcelize
import org.mozilla.focus.R
import org.mozilla.rocket.content.travel.ui.TravelBucketListFragment
import org.mozilla.rocket.content.travel.ui.TravelExploreFragment

class TravelTabsAdapter(
    fm: FragmentManager,
    activity: FragmentActivity,
    private val items: List<TabItem> = getDefaultTabs()
) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

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
                TYPE_EXPLORE -> TravelExploreFragment()
                TYPE_BUCKET_LIST -> TravelBucketListFragment()
                else -> error("Unsupported travel tab item type $type")
            }
        }
    }

    sealed class Tab(val item: Int) : Parcelable {
        @Parcelize object Explore : Tab(TYPE_EXPLORE)
        @Parcelize object BucketList : Tab(TYPE_BUCKET_LIST)
    }

    companion object {
        const val TYPE_EXPLORE = 0
        const val TYPE_BUCKET_LIST = 1

        private fun getDefaultTabs(): List<TabItem> = listOf(
            TabItem(TYPE_EXPLORE, R.string.travel_vertical_category_1),
            TabItem(TYPE_BUCKET_LIST, R.string.travel_vertical_category_2)
        )
    }
}