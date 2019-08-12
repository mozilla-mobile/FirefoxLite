package org.mozilla.rocket.shopping.search.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

class ShoppingSearchTabsAdapter(
    fm: FragmentManager,
    private val items: List<TabItem>
) : FragmentPagerAdapter(fm) {

    override fun getItem(position: Int): Fragment = items[position].fragment

    override fun getCount(): Int = items.size

    override fun getPageTitle(position: Int): CharSequence? = items[position].title

    data class TabItem(
        val fragment: Fragment,
        val title: String
    )
}