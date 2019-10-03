package org.mozilla.rocket.content.ecommerce.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import dagger.Lazy
import kotlinx.android.synthetic.main.activity_shopping.*
import org.mozilla.focus.R
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.ecommerce.ui.adapter.ShoppingTabsAdapter
import org.mozilla.rocket.content.getViewModel
import javax.inject.Inject

class ShoppingActivity : FragmentActivity() {

    @Inject
    lateinit var shoppingViewModelCreator: Lazy<ShoppingViewModel>

    private lateinit var shoppingViewModel: ShoppingViewModel
    private lateinit var adapter: ShoppingTabsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        shoppingViewModel = getViewModel(shoppingViewModelCreator)
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
    }

    private fun observeRefreshAction() {
        refresh_button.setOnClickListener {
            shoppingViewModel.refresh()
        }
    }

    companion object {
        fun getStartIntent(context: Context) =
            Intent(context, ShoppingActivity::class.java).also { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    }
}