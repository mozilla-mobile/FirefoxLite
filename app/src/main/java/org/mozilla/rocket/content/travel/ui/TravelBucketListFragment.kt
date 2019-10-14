package org.mozilla.rocket.content.travel.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import dagger.Lazy
import kotlinx.android.synthetic.main.fragment_travel_bucket_list.*
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.common.ui.VerticalSpaceItemDecoration
import org.mozilla.rocket.content.getActivityViewModel
import org.mozilla.rocket.content.travel.ui.adapter.BucketListCityAdapterDelegate
import org.mozilla.rocket.content.travel.ui.adapter.BucketListCityUiModel
import javax.inject.Inject

class TravelBucketListFragment : Fragment() {

    @Inject
    lateinit var travelBucketListViewModelCreator: Lazy<TravelBucketListViewModel>

    private lateinit var travelBucketListViewModel: TravelBucketListViewModel
    private lateinit var bucketListAdapter: DelegateAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        travelBucketListViewModel = getActivityViewModel(travelBucketListViewModelCreator)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_travel_bucket_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initBucketList()
        bindBucketListData()
        bindLoadingState()
        observeBucketListActions()
    }

    private fun initBucketList() {
        bucketListAdapter = DelegateAdapter(
                AdapterDelegatesManager().apply {
                    add(BucketListCityUiModel::class, R.layout.item_bucket_list, BucketListCityAdapterDelegate(travelBucketListViewModel))
                }
        )

        bucket_list_recycler_view.apply {
            val spaceWidth = resources.getDimensionPixelSize(R.dimen.card_space_width)
            addItemDecoration(VerticalSpaceItemDecoration(spaceWidth))

            adapter = bucketListAdapter
        }

        bucket_list_empty_explore.setOnClickListener {
            // TODO hook to travel view model for go search action
        }
    }

    private fun bindBucketListData() {

        travelBucketListViewModel.items.observe(this, Observer {
            bucketListAdapter.setData(it)
        })

        travelBucketListViewModel.isListEmpty.observe(this, Observer { isEmpty ->
            bucket_list_empty_view.isVisible = isEmpty
        })
    }

    private fun bindLoadingState() {
        travelBucketListViewModel.isDataLoading.observe(this@TravelBucketListFragment, Observer { state ->
            when (state) {
                is TravelBucketListViewModel.State.Idle -> showContentView()
                is TravelBucketListViewModel.State.Loading -> showLoadingView()
                is TravelBucketListViewModel.State.Error -> showErrorView()
            }
        })
    }

    private fun observeBucketListActions() {

        travelBucketListViewModel.openCity.observe(this, Observer {
            // TODO go city detail activity
        })

        // TODO observe go search event from travel view model
    }

    private fun showLoadingView() {
        spinner.isVisible = true
        bucket_list_recycler_view.isVisible = false
    }

    private fun showContentView() {
        spinner.isVisible = false
        bucket_list_recycler_view.isVisible = true
    }

    private fun showErrorView() {
        TODO("not implemented")
    }
}