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
import kotlinx.android.synthetic.main.fragment_challenge_list.content_layout
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
        bindListData()
        bindChallengeListViewState()
    }

    private fun initRecyclerView() {
        adapter = DelegateAdapter(
            AdapterDelegatesManager().apply {
                add(MissionUiModel.UnjoinedMission::class, R.layout.item_unjoined_mission, UnjoinedMissionsAdapterDelegate(missionViewModel))
                add(MissionUiModel.JoinedMission::class, R.layout.item_joined_mission, JoinedMissionsAdapterDelegate(missionViewModel))
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

    private fun bindListData() {
        missionViewModel.challengeList.observe(viewLifecycleOwner, Observer {
            adapter.setData(it)
        })
        missionViewModel.isChallengeListEmpty.observe(viewLifecycleOwner, Observer { isEmpty ->
            if (isEmpty) {
                showEmptyView()
            } else {
                showContentView()
            }
        })
    }

    private fun showContentView() {
        recycler_view.isVisible = true
        empty_view.isVisible = false
    }

    private fun showEmptyView() {
        recycler_view.isVisible = false
        empty_view.isVisible = true
    }

    private fun bindChallengeListViewState() {
        missionViewModel.challengeListViewState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is MissionViewModel.State.Loaded -> showLoaded()
                is MissionViewModel.State.Loading -> showLoading()
                is MissionViewModel.State.NoConnectionError -> showNoConnectionErrorView()
                is MissionViewModel.State.UnknownError -> showUnknownErrorView()
            }
        })
    }

    private fun showLoaded() {
        content_layout.isVisible = true
        error_view.isVisible = false
        loading_view.isVisible = false
    }

    private fun showLoading() {
        content_layout.isVisible = false
        error_view.isVisible = false
        loading_view.isVisible = true
    }

    private fun showNoConnectionErrorView() {
        content_layout.isVisible = false
        error_view.isVisible = true
        loading_view.isVisible = false

        error_text.text = resources.getText(R.string.msrp_reward_challenge_nointernet)
    }

    private fun showUnknownErrorView() {
        content_layout.isVisible = false
        error_view.isVisible = true
        loading_view.isVisible = false

        error_text.text = resources.getText(R.string.msrp_reward_challenge_error)
    }
}