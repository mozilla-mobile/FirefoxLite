package org.mozilla.rocket.content.news

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.design.widget.TabLayout.MODE_SCROLLABLE
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.mozilla.focus.R
import java.util.ArrayList

/**
 * Fragment that host the tabs for different types of content portal
 *
 */
class NewsTabFragment : Fragment() {

    private var bottomSheetBehavior: org.mozilla.rocket.widget.BottomSheetBehavior<View>? = null

    companion object {
        fun newInstance(): NewsTabFragment {
            return NewsTabFragment()
        }

        fun newInstance(bottomSheetBehavior: org.mozilla.rocket.widget.BottomSheetBehavior<View>?): NewsTabFragment {
            return NewsTabFragment().also { it.bottomSheetBehavior = bottomSheetBehavior }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.content_tab_news, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            ArrayList<String>().apply {
                add("top")
                add("sport")
                add("news")
                add("funny")
                add("game")
                add("test")
                add("software")
                add("3c")
                add("entertainment")
                add("house")
                add("cooking")
                add("travel")
                add("fashion")
            }.apply {
                val pager = view.findViewById<ViewPager>(R.id.news_viewpager)
                view.findViewById<TabLayout>(R.id.news_tab).run {
                    setupWithViewPager(pager)
                    tabMode = MODE_SCROLLABLE
                }
                pager.adapter = EcFragmentAdapter(childFragmentManager, this)
                pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                    override fun onPageScrollStateChanged(p0: Int) {
                    }

                    override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {
                    }

                    override fun onPageSelected(p0: Int) {
                        // need to call request Layout to force BottomsheetBehaviour to call our
                        // findScrollingChild() implementation to find the corresponding scrolling child
                        view.requestLayout()
                    }
                })
            }
        }
    }

    /**
     * Adapter that builds a page for each E-Commerce type .
     */
    inner class EcFragmentAdapter(fm: FragmentManager, private val cats: ArrayList<String>) : FragmentPagerAdapter(fm) {

        override fun getCount() = cats.size

        override fun getItem(position: Int): Fragment {
            val cat = cats[position]
            return NewsFragment.newInstance(cat)
        }

        override fun getPageTitle(position: Int): CharSequence {
            return cats[position]
        }
    }
}