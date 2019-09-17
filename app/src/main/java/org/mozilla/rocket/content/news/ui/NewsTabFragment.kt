package org.mozilla.rocket.content.news.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import dagger.Lazy
import kotlinx.android.synthetic.main.content_tab_news.*
import org.mozilla.focus.R
import org.mozilla.focus.activity.SettingsActivity
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.lite.partner.NewsItem
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.getActivityViewModel
import org.mozilla.rocket.content.news.data.NewsCategory
import org.mozilla.rocket.content.news.data.NewsLanguage
import org.mozilla.rocket.content.portal.ContentFeature
import javax.inject.Inject

class NewsTabFragment : Fragment() {

    @Inject
    lateinit var newsTabViewModelCreator: Lazy<NewsTabViewModel>

    private lateinit var newsTabViewModel: NewsTabViewModel

    private var newsSettings: Pair<NewsLanguage, List<NewsCategory>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
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
            newsTabViewModel = getActivityViewModel(newsTabViewModelCreator)

            newsTabViewModel.uiModel.observe(viewLifecycleOwner, Observer { settings ->
                settings?.let {
                    if (newsSettings != it.newsSettings) {
                        newsSettings = it.newsSettings
                        setupViewPager(view, it.newsSettings)
                    }
                }
            })
        }

        news_setting.setOnClickListener {
            setting()
        }

        news_refresh_button.setOnClickListener {
            newsSettings?.let {
                newsTabViewModel.refresh()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ContentFeature.SETTING_REQUEST_CODE) {
            newsTabViewModel.getNewsSettings()
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

    private fun setupViewPager(view: View, newsSettings: Pair<NewsLanguage, List<NewsCategory>>) {
        newsSettings.apply {
            val pager = view.findViewById<ViewPager>(R.id.news_viewpager)
            view.findViewById<TabLayout>(R.id.news_tab).run {
                setupWithViewPager(pager)
                tabMode = TabLayout.MODE_SCROLLABLE
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
                }
            })
        }
    }

    companion object {
        fun newInstance(): NewsTabFragment {
            return NewsTabFragment()
        }
    }

    interface NewsListingEventListener {
        fun onItemClicked(url: String)
        fun onStatus(items: List<NewsItem>?)
    }

    /**
     * Adapter that builds a page for each news category.
     */
    @Suppress("DEPRECATION")
    inner class EcFragmentAdapter(fm: FragmentManager, newsSettings: Pair<NewsLanguage, List<NewsCategory>>) :
        FragmentPagerAdapter(fm) {

        private val language = newsSettings.first.apiId
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
}
