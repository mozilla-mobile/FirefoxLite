package org.mozilla.rocket.msrp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_reward.*
import org.mozilla.focus.R
import org.mozilla.focus.navigation.ScreenNavigator
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.msrp.ui.adapter.RewardTabsAdapter

class RewardFragment : Fragment(), ScreenNavigator.RewardScreen {

    private lateinit var adapter: RewardTabsAdapter

    override fun getFragment() = this

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_reward, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViewPager()
        initTabLayout()
    }

    private fun initViewPager() {
        adapter = RewardTabsAdapter(requireFragmentManager(), requireActivity().resources)
        view_pager.apply {
            adapter = this@RewardFragment.adapter
        }
    }

    private fun initTabLayout() {
        reward_tabs.setupWithViewPager(view_pager)
    }
}