package org.mozilla.rocket.msrp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.Lazy
import kotlinx.android.synthetic.main.content_error_view.*
import kotlinx.android.synthetic.main.fragment_redeem_list.*
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.getActivityViewModel
import org.mozilla.rocket.msrp.ui.adapter.ExpiredMissionAdapterDelegate
import org.mozilla.rocket.msrp.ui.adapter.MissionUiModel
import org.mozilla.rocket.msrp.ui.adapter.RedeemableMissionAdapterDelegate
import org.mozilla.rocket.msrp.ui.adapter.RedeemedMissionAdapterDelegate
import javax.inject.Inject

class RedeemListFragment : Fragment() {

    @Inject
    lateinit var missionViewModelCreator: Lazy<MissionViewModel>

    private lateinit var missionViewModel: MissionViewModel
    private lateinit var adapter: DelegateAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        missionViewModel = getActivityViewModel(missionViewModelCreator)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_redeem_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        bindRedeemListViewState()
    }

    private fun initRecyclerView() {
        adapter = DelegateAdapter(
            AdapterDelegatesManager().apply {
                add(MissionUiModel.RedeemableMission::class, R.layout.item_redeemable_mission, RedeemableMissionAdapterDelegate())
                add(MissionUiModel.RedeemedMission::class, R.layout.item_redeemed_mission, RedeemedMissionAdapterDelegate())
                add(MissionUiModel.ExpiredMission::class, R.layout.item_expired_mission, ExpiredMissionAdapterDelegate())
            }
        )
        recycler_view.apply {
            adapter = this@RedeemListFragment.adapter
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        }
    }

    private fun bindRedeemListViewState() {
        missionViewModel.redeemListViewState.observe(this, Observer { state ->
            when (state) {
                is MissionViewModel.State.Loaded -> showMissionData(state.data)
                is MissionViewModel.State.Empty -> showEmptyView()
                is MissionViewModel.State.Loading -> showLoading()
                is MissionViewModel.State.NoConnectionError -> showNoConnectionErrorView()
                is MissionViewModel.State.UnknownError -> showUnknownErrorView()
            }
        })
    }

    private fun showMissionData(data: List<MissionUiModel>) {
        adapter.setData(data)
        recycler_view.isVisible = true
        empty_view.isVisible = false
        error_view.isVisible = false
        loading_view.isVisible = false
    }

    private fun showEmptyView() {
        recycler_view.isVisible = false
        empty_view.isVisible = true
        error_view.isVisible = false
        loading_view.isVisible = false
    }

    private fun showLoading() {
        recycler_view.isVisible = false
        empty_view.isVisible = false
        error_view.isVisible = false
        loading_view.isVisible = true
    }

    private fun showNoConnectionErrorView() {
        error_text.text = resources.getText(R.string.msrp_reward_challenge_nointernet)
        recycler_view.isVisible = false
        empty_view.isVisible = false
        error_view.isVisible = true
        loading_view.isVisible = false
    }

    private fun showUnknownErrorView() {
        error_text.text = resources.getText(R.string.msrp_reward_challenge_error)
        recycler_view.isVisible = false
        empty_view.isVisible = false
        error_view.isVisible = true
        loading_view.isVisible = false
    }
}