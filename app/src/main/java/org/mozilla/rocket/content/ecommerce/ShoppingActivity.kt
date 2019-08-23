package org.mozilla.rocket.content.ecommerce

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import kotlinx.android.synthetic.main.activity_shopping.*
import org.mozilla.focus.R
import org.mozilla.rocket.content.ecommerce.adapter.ShoppingTabsAdapter

class ShoppingActivity : FragmentActivity() {

    private lateinit var adapter: ShoppingTabsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shopping)
        initViewPager()
        initTabLayout()
    }

    private fun initViewPager() {
        adapter = ShoppingTabsAdapter(supportFragmentManager, this)
        view_pager.apply {
            adapter = this@ShoppingActivity.adapter
        }
    }

    private fun initTabLayout() {
        shopping_tabs.setupWithViewPager(view_pager)
    }

    companion object {
        fun getStartIntent(context: Context) = Intent(context, ShoppingActivity::class.java)
    }
}