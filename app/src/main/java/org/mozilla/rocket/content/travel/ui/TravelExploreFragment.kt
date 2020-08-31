package org.mozilla.rocket.content.travel.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import dagger.Lazy
import kotlinx.android.synthetic.main.fragment_travel_explore.*
import org.mozilla.focus.R
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.common.adapter.Runway
import org.mozilla.rocket.content.common.adapter.RunwayAdapterDelegate
import org.mozilla.rocket.content.common.adapter.RunwayItem
import org.mozilla.rocket.content.common.ui.ContentTabActivity
import org.mozilla.rocket.content.common.ui.RunwayViewModel
import org.mozilla.rocket.content.common.ui.VerticalTelemetryViewModel
import org.mozilla.rocket.content.getActivityViewModel
import org.mozilla.rocket.content.travel.ui.adapter.CityCategoryAdapterDelegate
import org.mozilla.rocket.content.travel.ui.adapter.CityCategoryUiModel
import org.mozilla.rocket.content.travel.ui.adapter.CitySearchAdapterDelegate
import org.mozilla.rocket.content.travel.ui.adapter.CitySearchUiModel
import javax.inject.Inject

class TravelExploreFragment : Fragment() {

    @Inject
    lateinit var travelExploreViewModelCreator: Lazy<TravelExploreViewModel>

    @Inject
    lateinit var runwayViewModelCreator: Lazy<RunwayViewModel>

    @Inject
    lateinit var telemetryViewModelCreator: Lazy<VerticalTelemetryViewModel>

    private lateinit var runwayViewModel: RunwayViewModel
    private lateinit var travelExploreViewModel: TravelExploreViewModel
    private lateinit var telemetryViewModel: VerticalTelemetryViewModel
    private lateinit var exploreAdapter: DelegateAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        runwayViewModel = getActivityViewModel(runwayViewModelCreator)
        travelExploreViewModel = getActivityViewModel(travelExploreViewModelCreator)
        telemetryViewModel = getActivityViewModel(telemetryViewModelCreator)
        travelExploreViewModel.requestExploreList()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_travel_explore, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initExplore()
        bindExploreData()
        bindLoadingState()
        observeExploreActions()
    }

    private fun initExplore() {
        exploreAdapter = DelegateAdapter(
                AdapterDelegatesManager().apply {
                    add(Runway::class, R.layout.item_runway_list, RunwayAdapterDelegate(runwayViewModel, TelemetryWrapper.Extra_Value.EXPLORE, telemetryViewModel))
                    add(CitySearchUiModel::class, R.layout.city_search, CitySearchAdapterDelegate(travelExploreViewModel))
                    add(CityCategoryUiModel::class, R.layout.item_city_category, CityCategoryAdapterDelegate(travelExploreViewModel, telemetryViewModel))
                }
        )
        explore_recycler_view.apply {
            adapter = exploreAdapter
        }
    }

    private fun bindExploreData() {
        travelExploreViewModel.exploreItems.observe(viewLifecycleOwner, Observer {
            exploreAdapter.setData(it)
            telemetryViewModel.updateVersionId(TelemetryWrapper.Extra_Value.EXPLORE, travelExploreViewModel.versionId)
        })
    }

    private fun bindLoadingState() {
        travelExploreViewModel.isDataLoading.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is TravelExploreViewModel.State.Idle -> showContentView()
                is TravelExploreViewModel.State.Loading -> showLoadingView()
                is TravelExploreViewModel.State.Error -> showErrorView()
            }
        })
    }

    private fun observeExploreActions() {
        runwayViewModel.openRunway.observe(viewLifecycleOwner, Observer { action ->
            context?.let {
                when (action.type) {
                    RunwayItem.TYPE_EXTERNAL_LINK -> {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(action.url))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }
                    else -> {
                        startActivity(ContentTabActivity.getStartIntent(
                            it,
                            action.url,
                            action.telemetryData.copy(vertical = TelemetryWrapper.Extra_Value.TRAVEL, versionId = travelExploreViewModel.versionId)))
                    }
                }
            }
        })
        travelExploreViewModel.openCity.observe(viewLifecycleOwner, Observer { city ->
            context?.let {
                startActivity(TravelCityActivity.getStartIntent(it, city, TelemetryWrapper.Extra_Value.EXPLORE))
            }
        })

        travelExploreViewModel.goSearch.observe(viewLifecycleOwner, Observer {
            context?.let {
                startActivity(TravelCitySearchActivity.getStartIntent(it))
            }
        })
    }

    private fun showLoadingView() {
        spinner.visibility = View.VISIBLE
        explore_recycler_view.visibility = View.GONE
    }

    private fun showContentView() {
        spinner.visibility = View.GONE
        explore_recycler_view.visibility = View.VISIBLE
    }

    private fun showErrorView() {
        TODO("not implemented")
    }
}
