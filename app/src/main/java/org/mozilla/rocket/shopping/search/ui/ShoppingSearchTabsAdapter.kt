package org.mozilla.rocket.shopping.search.ui

import android.util.SparseArray
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import org.mozilla.rocket.content.common.ui.ContentTabFragment
import org.mozilla.rocket.tabs.Session

@Suppress("DEPRECATION")
class ShoppingSearchTabsAdapter(
    fm: FragmentManager,
    private val items: List<TabItem>
) : FragmentStatePagerAdapter(fm) {

    // Sparse array to keep track of registered fragments in memory
    private val registeredFragments = SparseArray<Fragment>()

    override fun getItem(position: Int): Fragment = ContentTabFragment.newInstance(items[position].searchUrl, items[position].session)

    override fun getCount(): Int = items.size

    override fun getPageTitle(position: Int): CharSequence? = items[position].title

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val fragment = super.instantiateItem(container, position) as ContentTabFragment
        registeredFragments.put(position, fragment)
        return fragment
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        registeredFragments.remove(position)
        super.destroyItem(container, position, `object`)
    }

    fun getRegisteredFragment(position: Int): Fragment {
        return registeredFragments.get(position)
    }

    data class TabItem(
        val title: String,
        val searchUrl: String,
        val session: Session
    )
}