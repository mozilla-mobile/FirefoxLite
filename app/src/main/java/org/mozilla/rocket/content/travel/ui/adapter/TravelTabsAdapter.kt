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

    override fun getItem(position: Int): Fragment = items[position].fragment

    override fun getCount(): Int = items.size

    override fun getPageTitle(position: Int): CharSequence? = resource.getString(items[position].titleResId)

    data class TabItem(
        val fragment: Fragment,
        val titleResId: Int
    )

    sealed class Tab(val item: Int) : Parcelable {
        @Parcelize object Explore : Tab(0)
        @Parcelize object BucketList : Tab(1)
    }

    companion object {
        private fun getDefaultTabs(): List<TabItem> = listOf(
            TabItem(TravelExploreFragment(), R.string.travel_vertical_category_1),
            TabItem(TravelBucketListFragment(), R.string.travel_vertical_category_2)
        )
    }
}