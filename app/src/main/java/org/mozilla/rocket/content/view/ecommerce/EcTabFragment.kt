package org.mozilla.rocket.content.view.ecommerce

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.mozilla.focus.R
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.content.ContentFeature
import org.mozilla.rocket.content.ContentFeature.Companion.TYPE_COUPON
import org.mozilla.rocket.content.ContentFeature.Companion.TYPE_NEWS
import org.mozilla.rocket.content.ContentFeature.Companion.TYPE_TICKET
import org.mozilla.rocket.content.view.news.NewsFragment
import java.lang.IllegalStateException
import java.util.ArrayList

/**
 * Fragment that host the tabs for different types of content portal
 *
 */
class EcTabFragment : Fragment() {

    companion object {
        fun newInstance(): EcTabFragment {
            return EcTabFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.content_tab, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            ContentFeature().features().apply {
                val pager = view.findViewById<ViewPager>(R.id.content_viewpager)
                view.findViewById<TabLayout>(R.id.content_tab).setupWithViewPager(pager)
                pager.adapter = ContentFragmentAdapter(childFragmentManager, this)
                pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                    override fun onPageScrollStateChanged(p0: Int) {
                    }

                    override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {
                    }

                    override fun onPageSelected(p0: Int) {
                        if (this@apply.size > p0 && this@apply[p0] == TYPE_COUPON) {
                            TelemetryWrapper.openLifeFeedPromo(TelemetryWrapper.Extra_Value.TAB)
                        } else {
                            TelemetryWrapper.openLifeFeedEc()
                        }
                        // need to call request Layout to force BottomsheetBehaviour to call our
                        // findScrollingChild() implementation to find the corresponding scrolling child
                        view.requestLayout()
                    }
                })
            }
        }
    }

    /**
     * Adapter that builds a page for each content type .
     */
    inner class ContentFragmentAdapter(fm: FragmentManager, private val features: ArrayList<Int>) : FragmentPagerAdapter(fm) {

        override fun getCount() = features.size

        override fun getItem(position: Int): Fragment {
            val feature = features[position]
            return when (feature) {
                TYPE_NEWS -> NewsFragment()
                TYPE_TICKET -> EcFragment.newInstance(TYPE_TICKET)
                TYPE_COUPON -> EcFragment.newInstance(TYPE_COUPON)
                else -> throw IllegalStateException("Not supported Content Type")
            }
        }

        override fun getPageTitle(position: Int): CharSequence {
            val feature = features[position]
            return when (feature) {
                TYPE_NEWS -> context?.getString(R.string.label_menu_latest_top_trending_news) ?: ""
                TYPE_TICKET -> context?.getString(R.string.label_menu_e_commerce) ?: ""
                TYPE_COUPON -> context?.getString(R.string.label_menu_e_commerce_coupon) ?: ""
                else -> throw IllegalStateException("Not supported Content Type")
            }
        }
    }
}