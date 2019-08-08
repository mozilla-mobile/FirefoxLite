package org.mozilla.rocket.shopping.search.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

class ShoppingSearchTabsAdapter(
    fm: FragmentManager,
    private val items: List<TabItem> = DEFAULT_TABS
) : FragmentPagerAdapter(fm) {

    override fun getItem(position: Int): Fragment = items[position].fragment

    override fun getCount(): Int = items.size

    override fun getPageTitle(position: Int): CharSequence? = items[position].title

    data class TabItem(
        val fragment: Fragment,
        val title: String
    )

    companion object {
        private val DEFAULT_TABS: List<TabItem> by lazy {
            listOf(
                TabItem(ShoppingSearchResultContentFragment.newInstance("https://www.bukalapak.com"), "Bukalapak"),
                TabItem(ShoppingSearchResultContentFragment.newInstance("https://tokopedia.com"), "Tokopedia"),
                TabItem(ShoppingSearchResultContentFragment.newInstance("https://www.jd.id"), "JD.ID")
            )
        }
    }
}