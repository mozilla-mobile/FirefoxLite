package org.mozilla.rocket.content.ecommerce.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import org.mozilla.rocket.content.ecommerce.ui.ShoppingViewModel

@Suppress("DEPRECATION")
class ShoppingTabsAdapter(
    fm: FragmentManager,
    activity: FragmentActivity,
    private val items: List<ShoppingViewModel.ShoppingTabItem>
) : FragmentStatePagerAdapter(fm) {

    private val resource = activity.resources

    override fun getItem(position: Int): Fragment = items[position].createFragment()

    override fun getCount(): Int = items.size

    override fun getPageTitle(position: Int): CharSequence? = resource.getString(items[position].titleResId)
}