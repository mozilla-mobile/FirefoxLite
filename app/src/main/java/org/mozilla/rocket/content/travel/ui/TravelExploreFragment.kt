package org.mozilla.rocket.content.travel.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import dagger.Lazy
import kotlinx.android.synthetic.main.fragment_travel_explore.*
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.common.adapter.Runway
import org.mozilla.rocket.content.common.adapter.RunwayAdapterDelegate
import org.mozilla.rocket.content.common.ui.ContentTabActivity
import org.mozilla.rocket.content.common.ui.RunwayViewModel
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

    private lateinit var runwayViewModel: RunwayViewModel
    private lateinit var travelExploreViewModel: TravelExploreViewModel
    private lateinit var exploreAdapter: DelegateAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        runwayViewModel = getActivityViewModel(runwayViewModelCreator)
        travelExploreViewModel = getActivityViewModel(travelExploreViewModelCreator)
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
                    add(Runway::class, R.layout.item_runway_list, RunwayAdapterDelegate(runwayViewModel))
                    add(CitySearchUiModel::class, R.layout.city_search, CitySearchAdapterDelegate(travelExploreViewModel))
                    add(CityCategoryUiModel::class, R.layout.item_city_category, CityCategoryAdapterDelegate(travelExploreViewModel))
                }
        )
        explore_recycler_view.apply {
            adapter = exploreAdapter
        }
    }

    private fun bindExploreData() {
        travelExploreViewModel.items.observe(this, Observer {
            exploreAdapter.setData(it)
        })
    }

    private fun bindLoadingState() {
        travelExploreViewModel.isDataLoading.observe(this@TravelExploreFragment, Observer { state ->
            when (state) {
                is TravelExploreViewModel.State.Idle -> showContentView()
                is TravelExploreViewModel.State.Loading -> showLoadingView()
                is TravelExploreViewModel.State.Error -> showErrorView()
            }
        })
    }

    private fun observeExploreActions() {

        runwayViewModel.openRunway.observe(this, Observer { action ->
            context?.let {
                startActivity(ContentTabActivity.getStartIntent(it, action.url))
            }
        })
        travelExploreViewModel.openCity.observe(this, Observer { name ->
            context?.let {
                startActivity(TravelCityActivity.getStartIntent(it, name))
            }
        })

        travelExploreViewModel.goSearch.observe(this, Observer {
            context?.let {
                startActivity(TravelCitySearchActivity.getStartIntent(it))
            }
        })
    }

    private fun showLoadingView() {
        spinner.visibility = View.VISIBLE
    }

    private fun showContentView() {
        spinner.visibility = View.GONE
    }

    private fun showErrorView() {
        TODO("not implemented")
    }
}
