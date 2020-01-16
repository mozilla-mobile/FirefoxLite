package org.mozilla.rocket.content.travel.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.os.Parcelable
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import com.google.android.material.tabs.TabLayout
import dagger.Lazy
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.activity_travel.*
import org.mozilla.focus.R
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.common.data.ContentTabTelemetryData
import org.mozilla.rocket.content.common.ui.ContentTabActivity
import org.mozilla.rocket.content.common.ui.VerticalTelemetryViewModel
import org.mozilla.rocket.content.getViewModel
import org.mozilla.rocket.content.travel.ui.adapter.TravelTabsAdapter
import org.mozilla.rocket.content.travel.ui.adapter.TravelTabsAdapter.Companion.TYPE_EXPLORE
import org.mozilla.rocket.content.travel.ui.adapter.TravelTabsAdapter.Tab
import org.mozilla.rocket.content.travel.ui.adapter.TravelTabsAdapter.Tab.Explore
import org.mozilla.rocket.extension.isLaunchedFromHistory
import org.mozilla.rocket.util.sha256
import javax.inject.Inject

class TravelActivity : FragmentActivity() {

    @Inject
    lateinit var travelViewModelCreator: Lazy<TravelViewModel>

    @Inject
    lateinit var travelExploreViewModelCreator: Lazy<TravelExploreViewModel>

    @Inject
    lateinit var telemetryViewModelCreator: Lazy<VerticalTelemetryViewModel>

    private lateinit var adapter: TravelTabsAdapter
    private lateinit var travelViewModel: TravelViewModel
    private lateinit var travelExploreViewModel: TravelExploreViewModel
    private lateinit var telemetryViewModel: VerticalTelemetryViewModel
    private lateinit var tab: Tab

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)

        travelViewModel = getViewModel(travelViewModelCreator)
        travelExploreViewModel = getViewModel(travelExploreViewModelCreator)
        telemetryViewModel = getViewModel(telemetryViewModelCreator)

        setContentView(R.layout.activity_travel)

        tab = intent?.extras?.getParcelable(EXTRA_DEFAULT_TAB) ?: Explore

        initViewPager()
        initTabLayout()
        initToolBar()

        intent.extras?.let {
            if (!isLaunchedFromHistory()) {
                parseDeepLink(it)
            }
        }
    }

    private fun parseDeepLink(bundle: Bundle): Boolean {
        when (bundle.getString(EXTRA_DEEP_LINK) ?: return false) {
            DEEP_LINK_TRAVEL_ITEM_PAGE -> openTravelItemPageFromDeepLink(bundle.getParcelable(EXTRA_TRAVEL_ITEM_DATA)!!)
        }

        return true
    }

    private fun openTravelItemPageFromDeepLink(travelItemData: DeepLink.TravelItemPage.Data) {
        val telemetryData = ContentTabTelemetryData(
            TelemetryWrapper.Extra_Value.TRAVEL,
            travelItemData.feed,
            travelItemData.source,
            "", // no need when triggered by deep link
            travelItemData.url.sha256(),
            "", // no need when triggered by deep link
            System.currentTimeMillis()
        )
        startActivity(ContentTabActivity.getStartIntent(this, travelItemData.url, telemetryData))
    }

    override fun onResume() {
        super.onResume()
        telemetryViewModel.onSessionStarted(TelemetryWrapper.Extra_Value.TRAVEL)
    }

    override fun onPause() {
        super.onPause()
        telemetryViewModel.onSessionEnded()
    }

    private fun initViewPager() {
        travelViewModel.loadTabs.observe(this, Observer {
            adapter = TravelTabsAdapter(supportFragmentManager, this)
            view_pager.apply {
                adapter = this@TravelActivity.adapter
                currentItem = tab.item
            }
        })

        Looper.myQueue().addIdleHandler {
            travelViewModel.initTabs()
            false
        }
    }

    private fun initTabLayout() {
        travel_tabs.setupWithViewPager(view_pager)
        travel_tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab) = Unit

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit

            override fun onTabSelected(tab: TabLayout.Tab) {
                val category = when (tab.position) {
                    TYPE_EXPLORE -> TelemetryWrapper.Extra_Value.EXPLORE
                    else -> TelemetryWrapper.Extra_Value.BUCKET_LIST
                }

                telemetryViewModel.onCategorySelected(category)
            }
        })
    }

    private fun initToolBar() {
        refresh_button.setOnClickListener {
            travelViewModel.onRefreshClicked()
            telemetryViewModel.onRefreshClicked()
        }
    }

    sealed class DeepLink(val name: String) {
        data class TravelItemPage(val data: Data) : DeepLink(DEEP_LINK_TRAVEL_ITEM_PAGE) {
            @Parcelize
            data class Data(val url: String, val feed: String, val source: String) : Parcelable
        }
    }

    companion object {
        private const val EXTRA_DEFAULT_TAB = "default_tab"

        private const val EXTRA_DEEP_LINK = "extra_deep_link"
        private const val EXTRA_TRAVEL_ITEM_DATA = "extra_travel_item_data"
        private const val DEEP_LINK_TRAVEL_ITEM_PAGE = "deep_link_travel_item_page"

        fun getStartIntent(context: Context, tab: Tab = Explore) =
                Intent(context, TravelActivity::class.java).also {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    it.putExtra(EXTRA_DEFAULT_TAB, tab)
                }

        fun getStartIntent(context: Context, deepLink: DeepLink) = getStartIntent(context).apply {
            putExtras(Bundle().apply {
                putString(EXTRA_DEEP_LINK, deepLink.name)
                when (deepLink) {
                    is DeepLink.TravelItemPage -> {
                        putParcelable(EXTRA_TRAVEL_ITEM_DATA, deepLink.data)
                    }
                }
            })
        }
    }
}
