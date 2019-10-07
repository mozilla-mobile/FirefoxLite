package org.mozilla.rocket.content.ecommerce.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import dagger.Lazy
import kotlinx.android.synthetic.main.fragment_deal.*
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.common.adapter.Runway
import org.mozilla.rocket.content.common.adapter.RunwayAdapterDelegate
import org.mozilla.rocket.content.common.ui.ContentTabActivity
import org.mozilla.rocket.content.common.ui.RunwayViewModel
import org.mozilla.rocket.content.ecommerce.ui.adapter.ProductCategory
import org.mozilla.rocket.content.ecommerce.ui.adapter.ProductCategoryAdapterDelegate
import org.mozilla.rocket.content.getActivityViewModel
import javax.inject.Inject

class DealFragment : Fragment() {

    @Inject
    lateinit var runwayViewModelCreator: Lazy<RunwayViewModel>

    @Inject
    lateinit var dealViewModelCreator: Lazy<DealViewModel>

    private lateinit var runwayViewModel: RunwayViewModel
    private lateinit var dealViewModel: DealViewModel
    private lateinit var dealAdapter: DelegateAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        runwayViewModel = getActivityViewModel(runwayViewModelCreator)
        dealViewModel = getActivityViewModel(dealViewModelCreator)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_deal, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDeals()
        bindListData()
        bindPageState()
        observeAction()
        initNoResultView()
    }

    private fun initDeals() {
        dealAdapter = DelegateAdapter(
            AdapterDelegatesManager().apply {
                add(Runway::class, R.layout.item_runway_list, RunwayAdapterDelegate(runwayViewModel))
                add(ProductCategory::class, R.layout.item_product_category, ProductCategoryAdapterDelegate(dealViewModel))
            }
        )
        content_deals.apply {
            adapter = dealAdapter
        }
    }

    private fun bindListData() {
        dealViewModel.dealItems.observe(this@DealFragment, Observer {
            dealAdapter.setData(it)
        })
    }

    private fun bindPageState() {
        dealViewModel.isDataLoading.observe(this@DealFragment, Observer { state ->
            when (state) {
                is DealViewModel.State.Idle -> showContentView()
                is DealViewModel.State.Loading -> showLoadingView()
                is DealViewModel.State.Error -> showErrorView()
            }
        })
    }

    private fun observeAction() {
        runwayViewModel.openRunway.observe(this, Observer { linkUrl ->
            context?.let {
                startActivity(ContentTabActivity.getStartIntent(it, linkUrl))
            }
        })

        dealViewModel.openProduct.observe(this, Observer { linkUrl ->
            context?.let {
                startActivity(ContentTabActivity.getStartIntent(it, linkUrl))
            }
        })
    }

    private fun initNoResultView() {
        no_result_view.setButtonOnClickListener(View.OnClickListener {
            dealViewModel.onRetryButtonClicked()
        })
    }

    private fun showLoadingView() {
        spinner.visibility = View.VISIBLE
        content_deals.visibility = View.GONE
        no_result_view.visibility = View.GONE
    }

    private fun showContentView() {
        spinner.visibility = View.GONE
        content_deals.visibility = View.VISIBLE
        no_result_view.visibility = View.GONE
    }

    private fun showErrorView() {
        spinner.visibility = View.GONE
        content_deals.visibility = View.GONE
        no_result_view.visibility = View.VISIBLE
    }
}