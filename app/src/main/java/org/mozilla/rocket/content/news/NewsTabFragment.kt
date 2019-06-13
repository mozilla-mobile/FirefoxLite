package org.mozilla.rocket.content.news

import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.design.widget.TabLayout.MODE_SCROLLABLE
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.app.FragmentTransaction
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.android.support.AndroidSupportInjection
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.content_tab_news.*
import org.mozilla.focus.R
import org.mozilla.focus.activity.SettingsActivity
import org.mozilla.rocket.content.ContentPortalViewState
import org.mozilla.rocket.content.activityViewModelProvider
import org.mozilla.rocket.content.portal.ContentFeature
import org.mozilla.rocket.content.portal.ContentPortalView

/**
 * Fragment that host the tabs for different types of content portal
 *
 */
class NewsTabFragment : DaggerFragment() {

    @javax.inject.Inject
    lateinit var viewModelFactory: NewsViewModelFactory

    lateinit var newsViewModel: NewsViewModel

    private var bottomSheetBehavior: org.mozilla.rocket.widget.BottomSheetBehavior<View>? = null

    companion object {
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
        AndroidSupportInjection.inject(this)

        if (savedInstanceState == null) {
            newsViewModel = activityViewModelProvider(viewModelFactory)

            newsViewModel.categories.observe(viewLifecycleOwner, Observer { list ->
                list?.let {
                    setupViewPager(view, it)
                }
            })
        }

        news_setting.setOnClickListener {
            setting()
        }
    }

    private fun setupViewPager(view: View, newsCategories: List<String>) {
        newsCategories.apply {
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
                    ContentPortalViewState.lastNewsTab = p0
                    // need to call request Layout to force BottomsheetBehaviour to call our
                    // findScrollingChild() implementation to find the corresponding scrolling child
                    view.requestLayout()
                }
            })
            ContentPortalViewState.lastNewsTab?.let {
                pager.currentItem = it
            }
        }
    }

    /**
     * Adapter that builds a page for each E-Commerce type .
     */
    inner class EcFragmentAdapter(fm: FragmentManager, private val cats: List<String>) : FragmentPagerAdapter(fm) {

        override fun getCount() = cats.size

        override fun getItem(position: Int): Fragment {
            val cat = cats[position]
            return NewsFragment.newInstance(cat)
        }

        override fun getPageTitle(position: Int): CharSequence {
            return cats[position]
        }
    }

    fun setting() {

        val intent = Intent().run {
            putExtra(ContentFeature.EXTRA_CONFIG_NEWS, "config")
            setClass(context!!, SettingsActivity::class.java)
        }
        startActivityForResult(intent, ContentFeature.SETTING_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ContentFeature.SETTING_REQUEST_CODE) {
            // TODO: check if the setting has changed before recreate self
            // TODO: repeated code for initNewsTabFragment
            // recreate self
            context?.inTransaction {
                replace(
                    R.id.bottom_sheet, NewsTabFragment.newInstance(bottomSheetBehavior),
                    ContentPortalView.TAG_NEWS_FRAGMENT
                )
            }
        }
    }

    // TODO: make this a util
    // helper method to work with FragmentManager
    private inline fun Context.inTransaction(func: FragmentTransaction.() -> FragmentTransaction) {
        val fragmentManager = (this as? FragmentActivity)?.supportFragmentManager
        if (fragmentManager?.isStateSaved == true) {
            return
        }
        fragmentManager?.beginTransaction()?.func()?.commit()
    }
}