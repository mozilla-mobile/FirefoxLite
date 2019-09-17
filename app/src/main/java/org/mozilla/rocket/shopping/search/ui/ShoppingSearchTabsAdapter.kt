package org.mozilla.rocket.shopping.search.ui

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import org.mozilla.rocket.content.common.ui.ContentTabFragment

@Suppress("DEPRECATION")
class ShoppingSearchTabsAdapter(
    fm: FragmentManager,
    private val items: List<TabItem>
) : FragmentPagerAdapter(fm) {

    override fun getItem(position: Int): Fragment = ContentTabFragment.newInstance(items[position].searchUrl)

    override fun getCount(): Int = items.size

    override fun getPageTitle(position: Int): CharSequence? = items[position].title

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val fragment = super.instantiateItem(container, position) as ContentTabFragment
        // Force to update the search url since Viewpager may reuse the fragment instance previously instantiated.
        fragment.arguments?.apply {
            val item = items[position]
            putString(ContentTabFragment.EXTRA_URL, item.searchUrl)
        }
        return fragment
    }

    data class TabItem(
        val title: String,
        val searchUrl: String
    )
}