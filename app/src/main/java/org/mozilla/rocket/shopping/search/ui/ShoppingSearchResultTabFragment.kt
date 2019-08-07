package org.mozilla.rocket.shopping.search.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import kotlinx.android.synthetic.main.fragment_shopping_search_result_tab.shopping_search_tabs
import kotlinx.android.synthetic.main.fragment_shopping_search_result_tab.view_pager
import org.mozilla.focus.R

class ShoppingSearchResultTabFragment : Fragment() {

    private val safeArgs: ShoppingSearchResultTabFragmentArgs by navArgs()
    private val searchKeyword by lazy { safeArgs.searchKeyword }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_shopping_search_result_tab, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initViewPager()
        initTabLayout()
    }

    private fun initViewPager() {
        view_pager.adapter = ShoppingSearchTabsAdapter(childFragmentManager)
    }

    private fun initTabLayout() {
        shopping_search_tabs.setupWithViewPager(view_pager)
    }
}