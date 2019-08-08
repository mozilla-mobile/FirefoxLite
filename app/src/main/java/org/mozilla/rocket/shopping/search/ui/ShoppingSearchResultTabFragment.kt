package org.mozilla.rocket.shopping.search.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.navArgs
import kotlinx.android.synthetic.main.fragment_shopping_search_result_tab.shopping_search_tabs
import kotlinx.android.synthetic.main.fragment_shopping_search_result_tab.view_pager
import org.mozilla.focus.R
import org.mozilla.rocket.shopping.search.data.ShoppingSearchSiteRepository
import org.mozilla.rocket.shopping.search.ui.ShoppingSearchTabsAdapter.TabItem

class ShoppingSearchResultTabFragment : Fragment() {

    private val viewModelFactory: ShoppingSearchResultViewModel.Factory by lazy {
        ShoppingSearchResultViewModel.Factory(ShoppingSearchSiteRepository())
    }
    private val viewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(ShoppingSearchResultViewModel::class.java)
    }
    private val safeArgs: ShoppingSearchResultTabFragmentArgs by navArgs()
    private val searchKeyword by lazy { safeArgs.searchKeyword }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_shopping_search_result_tab, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initViewPager()
        initTabLayout()

        viewModel.search(searchKeyword)
    }

    private fun initViewPager() {
        viewModel.uiModel.observe(this, Observer { uiModel ->
            val tabItems = uiModel.sites.map { site ->
                TabItem(
                    ShoppingSearchResultContentFragment.newInstance(site.searchUrl),
                    site.title
                )
            }
            view_pager.adapter = ShoppingSearchTabsAdapter(childFragmentManager, tabItems)
        })
    }

    private fun initTabLayout() {
        shopping_search_tabs.setupWithViewPager(view_pager)
    }
}