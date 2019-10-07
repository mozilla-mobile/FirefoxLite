package org.mozilla.rocket.content.travel.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentActivity
import dagger.Lazy
import kotlinx.android.synthetic.main.activity_travel.*
import org.mozilla.focus.R
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.getViewModel
import org.mozilla.rocket.content.travel.ui.adapter.TravelTabsAdapter
import javax.inject.Inject

class TravelActivity : FragmentActivity() {

    @Inject
    lateinit var travelViewModelCreator: Lazy<TravelViewModel>
    @Inject
    lateinit var travelExploreViewModelCreator: Lazy<TravelExploreViewModel>

    private lateinit var adapter: TravelTabsAdapter
    private lateinit var travelViewModel: TravelViewModel
    private lateinit var travelExploreViewModel: TravelExploreViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)

        travelViewModel = getViewModel(travelViewModelCreator)
        travelExploreViewModel = getViewModel(travelExploreViewModelCreator)

        setContentView(R.layout.activity_travel)

        initViewPager()
        initTabLayout()
        initToolBar()
        makeStatusBarTransparent()
    }

    private fun makeStatusBarTransparent() {
        var visibility = window.decorView.systemUiVisibility
        // do not overwrite existing value
        visibility = visibility or (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        window.decorView.systemUiVisibility = visibility
    }

    private fun initViewPager() {
        travelViewModel.loadTabs.observe(this, Observer {
            adapter = TravelTabsAdapter(supportFragmentManager, this)
            view_pager.apply {
                adapter = this@TravelActivity.adapter
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
        fun getStartIntent(context: Context) =
                Intent(context, TravelActivity::class.java).also { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    }
}
