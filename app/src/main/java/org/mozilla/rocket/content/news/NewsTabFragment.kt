package org.mozilla.rocket.content.news

import androidx.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.MODE_SCROLLABLE
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.FragmentTransaction
import androidx.viewpager.widget.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.android.support.AndroidSupportInjection
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.content_tab_news.news_setting
import org.mozilla.focus.R
import org.mozilla.focus.activity.SettingsActivity
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.content.ContentPortalViewState
import org.mozilla.rocket.content.activityViewModelProvider
import org.mozilla.rocket.content.news.data.NewsCategory
import org.mozilla.rocket.content.news.data.NewsLanguage
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

            newsViewModel.newsSettings.observe(viewLifecycleOwner, Observer { settings ->
                settings?.let {
                    newsViewModel.clear()
                    setupViewPager(view, it)
                }
            })
        }

        news_setting.setOnClickListener {
            setting()
        }
    }

    private fun setupViewPager(view: View, newsSettings: Pair<NewsLanguage, List<NewsCategory>>) {
        newsSettings.apply {
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
                    if (newsSettings.second.size > p0) {
                        TelemetryWrapper.openLifeFeedNews(newsSettings.second[p0].order.toString())
                    }
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
    @Suppress("DEPRECATION")
    inner class EcFragmentAdapter(fm: FragmentManager, newsSettings: Pair<NewsLanguage, List<NewsCategory>>) :
        FragmentPagerAdapter(fm) {

        private val language = newsSettings.first.getApiId()
        private val displayCategories = newsSettings.second.filter { it.isSelected }

        override fun getCount() = displayCategories.size

        override fun getItem(position: Int): Fragment {
            val cat = displayCategories[position]
            return NewsFragment.newInstance(cat.categoryId, language)
        }

        override fun getPageTitle(position: Int): CharSequence {
            return getString(displayCategories[position].stringResourceId)
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val fragment = super.instantiateItem(container, position) as NewsFragment
            // Force to update the news language and category settings since Viewpager may reuse
            // the fragment instance previously instantiated.
            fragment.arguments?.apply {
                val category = displayCategories[position]
                putString(ContentFeature.TYPE_KEY, category.categoryId)
                putString(ContentFeature.EXTRA_NEWS_LANGUAGE, language)
            }
            return fragment
        }
    }

    fun setting() {
        val intent = Intent().run {
            putExtra(ContentFeature.EXTRA_CONFIG_NEWS, "config")
            setClass(context!!, SettingsActivity::class.java)
        }
        TelemetryWrapper.clickOnNewsSetting()
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
