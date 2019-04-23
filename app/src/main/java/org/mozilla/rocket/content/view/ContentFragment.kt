package org.mozilla.rocket.content.view

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
import org.mozilla.rocket.content.TYPE_COUPON
import org.mozilla.rocket.content.TYPE_KEY
import org.mozilla.rocket.content.TYPE_NEWS
import org.mozilla.rocket.content.TYPE_TICKET
import org.mozilla.rocket.content.view.ecommerce.EcFragment
import org.mozilla.rocket.content.view.news.NewsFragment
import java.util.ArrayList

class ContentFragment : Fragment() {

    companion object {
        fun newInstance(features: ArrayList<Int>): ContentFragment {
            val args = Bundle().apply {
                putIntegerArrayList(TYPE_KEY, features)
            }
            return ContentFragment().apply { arguments = args }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.content_tab, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            arguments?.getIntegerArrayList(TYPE_KEY)?.apply {
                val pager = view.findViewById<ViewPager>(R.id.content_viewpager)
                view.findViewById<TabLayout>(R.id.content_tab).setupWithViewPager(pager)
                pager.adapter = ContentFragmentAdapter(childFragmentManager, this)
            }
        }
    }

    /**
     * Adapter that builds a page for each content type .
     */
    inner class ContentFragmentAdapter(
        fm: FragmentManager, private val features: ArrayList<Int>
    ) : FragmentPagerAdapter(fm) {


        override fun getCount() = features.size

        override fun getItem(position: Int): Fragment {
            val feature = features[position]
            return when (feature) {
                TYPE_NEWS -> NewsFragment()
                TYPE_TICKET -> EcFragment.newInstance(TYPE_TICKET)
                TYPE_COUPON -> EcFragment.newInstance(TYPE_COUPON)
                else -> NewsFragment()
            }
        }

        override fun getPageTitle(position: Int): CharSequence {
            val feature = features[position]
            return when (feature) {
                TYPE_NEWS -> "News"
                TYPE_TICKET -> context!!.getString(R.string.label_menu_e_commerce)
                TYPE_COUPON -> context!!.getString(R.string.label_menu_e_commerce_coupon)
                else -> "ERROR"
            }
        }
    }
}