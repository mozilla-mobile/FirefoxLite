package org.mozilla.rocket.content.ecommerce.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import com.google.android.material.tabs.TabLayout
import dagger.Lazy
import kotlinx.android.synthetic.main.activity_shopping.*
import org.mozilla.focus.R
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.common.ui.VerticalTelemetryViewModel
import org.mozilla.rocket.content.ecommerce.ui.adapter.ShoppingTabsAdapter
import org.mozilla.rocket.content.getViewModel
import javax.inject.Inject

class ShoppingActivity : FragmentActivity() {

    @Inject
    lateinit var shoppingViewModelCreator: Lazy<ShoppingViewModel>

    @Inject
    lateinit var telemetryViewModelCreator: Lazy<VerticalTelemetryViewModel>

    private lateinit var shoppingViewModel: ShoppingViewModel
    private lateinit var telemetryViewModel: VerticalTelemetryViewModel
    private lateinit var adapter: ShoppingTabsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        shoppingViewModel = getViewModel(shoppingViewModelCreator)
        telemetryViewModel = getViewModel(telemetryViewModelCreator)
        setContentView(R.layout.activity_shopping)
        initViewPager()
        initTabLayout()
        observeRefreshAction()
    }

    private fun initViewPager() {
        shoppingViewModel.shoppingTabItems.observe(this, Observer { items ->
            adapter = ShoppingTabsAdapter(supportFragmentManager, this, items)
            view_pager.apply {
                adapter = this@ShoppingActivity.adapter
            }
        })
    }

    private fun initTabLayout() {
        shopping_tabs.setupWithViewPager(view_pager)
        shopping_tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab) = Unit

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit

            override fun onTabSelected(tab: TabLayout.Tab) {
                val category = when (tab.position) {
                    ShoppingViewModel.ShoppingTabItem.TYPE_DEAL_TAB -> TelemetryWrapper.Extra_Value.SHOPPING_DEAL
                    ShoppingViewModel.ShoppingTabItem.TYPE_COUPON_TAB -> TelemetryWrapper.Extra_Value.SHOPPING_COUPON
                    else -> TelemetryWrapper.Extra_Value.SHOPPING_VOUCHER
                }
                telemetryViewModel.onCategorySelected(category)
            }
        })
    }

    private fun observeRefreshAction() {
        refresh_button.setOnClickListener {
            shoppingViewModel.refresh()
            telemetryViewModel.onRefreshClicked()
        }
    }

    override fun onResume() {
        super.onResume()
        telemetryViewModel.onSessionStarted(TelemetryWrapper.Extra_Value.SHOPPING)
    }

    override fun onPause() {
        super.onPause()
        telemetryViewModel.onSessionEnded()
    }

    companion object {
        fun getStartIntent(context: Context) =
            Intent(context, ShoppingActivity::class.java).also { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    }
}