package org.mozilla.rocket.content.travel.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import kotlinx.android.synthetic.main.activity_travel.*
import org.mozilla.focus.R
import org.mozilla.rocket.content.travel.ui.adapter.TravelTabsAdapter

class TravelActivity : FragmentActivity() {

    private lateinit var adapter: TravelTabsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_travel)
        initViewPager()
        initTabLayout()
        initToolBar()
    }

    private fun initViewPager() {
        adapter = TravelTabsAdapter(supportFragmentManager, this)
        view_pager.apply {
            adapter = this@TravelActivity.adapter
        }
    }

    private fun initTabLayout() {
        travel_tabs.setupWithViewPager(view_pager)
    }

    private fun initToolBar() {
        refresh_button.setOnClickListener {
            when (travel_tabs.selectedTabPosition) {
//                TAB_EXPLORE -> //TODO refresh explore data
//                TAB_BUCKET_LIST -> //TODO refresh bucket list
            }
        }
    }

    companion object {
        const val TAB_EXPLORE = 0
        const val TAB_BUCKET_LIST = 1
        fun getStartIntent(context: Context) =
                Intent(context, TravelActivity::class.java).also { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    }
}
