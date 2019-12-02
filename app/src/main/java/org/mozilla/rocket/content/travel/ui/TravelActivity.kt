package org.mozilla.rocket.content.travel.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import dagger.Lazy
import kotlinx.android.synthetic.main.activity_travel.*
import org.mozilla.focus.R
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.common.ui.VerticalTelemetryViewModel
import org.mozilla.rocket.content.getViewModel
import org.mozilla.rocket.content.travel.ui.adapter.TravelTabsAdapter
import org.mozilla.rocket.content.travel.ui.adapter.TravelTabsAdapter.Tab
import org.mozilla.rocket.content.travel.ui.adapter.TravelTabsAdapter.Tab.Explore
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
                setCurrentItem(tab.item)
            }
        })

        travelViewModel.initTabs()
    }

    private fun initTabLayout() {
        travel_tabs.setupWithViewPager(view_pager)
    }

    private fun initToolBar() {
        refresh_button.setOnClickListener {
            travelViewModel.onRefreshClicked()
        }
    }

    companion object {
        private const val EXTRA_DEFAULT_TAB = "default_tab"
        fun getStartIntent(context: Context, tab: Tab = Explore) =
                Intent(context, TravelActivity::class.java).also {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    it.putExtra(EXTRA_DEFAULT_TAB, tab)
                }
    }
}
