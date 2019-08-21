package org.mozilla.rocket.shopping.search.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import dagger.Lazy
import kotlinx.android.synthetic.main.fragment_shopping_search_result_tab.shopping_search_tabs
import kotlinx.android.synthetic.main.fragment_shopping_search_result_tab.view_pager
import org.mozilla.focus.R
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.getViewModel
import org.mozilla.rocket.shopping.search.ui.ShoppingSearchTabsAdapter.TabItem
import javax.inject.Inject

class ShoppingSearchResultTabFragment : Fragment() {

    @Inject
    lateinit var viewModelCreator: Lazy<ShoppingSearchResultViewModel>

    private lateinit var viewModel: ShoppingSearchResultViewModel

    private val safeArgs: ShoppingSearchResultTabFragmentArgs by navArgs()
    private val searchKeyword by lazy { safeArgs.searchKeyword }

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        viewModel = getViewModel { viewModelCreator.get() }
    }

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