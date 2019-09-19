package org.mozilla.rocket.msrp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_missions.*
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.msrp.ui.adapter.ChallengeAdapterDelegate
import org.mozilla.rocket.msrp.ui.adapter.ChallengeUiModel

class ChallengeListFragment : Fragment() {

    private lateinit var adapter: DelegateAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_missions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        prepareData()
    }

    private fun initRecyclerView() {
        adapter = DelegateAdapter(
            AdapterDelegatesManager().apply {
                add(ChallengeUiModel::class, R.layout.msrp_challenge_mission, ChallengeAdapterDelegate())
            }
        )
        recycler_view.apply {
            adapter = this@ChallengeListFragment.adapter
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        }
    }

    private fun prepareData() {
        val fakeChallenge = ChallengeUiModel(
            title = "7-day challenge for free VPN",
            expirationText = "Expires 02/08/2019",
            progress = 74,
            imageUrl = "http://www.gameloft.com/central/upload/Asphalt-9-Legends-Slider-logo-2.jpg",
            showRedDot = true
        )

        adapter.setData(listOf(fakeChallenge))
    }
}