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
import kotlinx.android.synthetic.main.content_error_view.error_text
import kotlinx.android.synthetic.main.content_error_view.retry_button
import kotlinx.android.synthetic.main.fragment_challenge_list.empty_view
import kotlinx.android.synthetic.main.fragment_challenge_list.error_view
import kotlinx.android.synthetic.main.fragment_challenge_list.loading_view
import kotlinx.android.synthetic.main.fragment_challenge_list.recycler_view
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.getActivityViewModel
import org.mozilla.rocket.msrp.ui.adapter.JoinedMissionsAdapterDelegate
import org.mozilla.rocket.msrp.ui.adapter.MissionUiModel
import org.mozilla.rocket.msrp.ui.adapter.UnjoinedMissionsAdapterDelegate
import javax.inject.Inject

class ChallengeListFragment : Fragment() {

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
        return inflater.inflate(R.layout.fragment_challenge_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        initErrorView()
        bindChallengesViewState()
    }

    private fun initRecyclerView() {
        adapter = DelegateAdapter(
            AdapterDelegatesManager().apply {
                add(MissionUiModel.UnjoinedMission::class, R.layout.item_unjoined_mission, UnjoinedMissionsAdapterDelegate())
                add(MissionUiModel.JoinedMission::class, R.layout.item_joined_mission, JoinedMissionsAdapterDelegate())
            }
        )
        recycler_view.apply {
            adapter = this@ChallengeListFragment.adapter
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        }
    }

    private fun initErrorView() {
        retry_button.setOnClickListener {
            missionViewModel.onRetryButtonClicked()
        }
    }

    private fun bindChallengesViewState() {
//        val fakeChallenge = MissionUiModel(
//            title = "7-day challenge for free VPN",
//            expirationTime = "Expires 02/08/2019",
//            progress = 74,
//            imageUrl = "http://www.gameloft.com/central/upload/Asphalt-9-Legends-Slider-logo-2.jpg",
//            showRedDot = true
//        )
        missionViewModel.challengeListViewState.observe(this, Observer { state ->
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