package org.mozilla.rocket.content.ecommerce.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import dagger.Lazy
import kotlinx.android.synthetic.main.fragment_coupon.*
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.ecommerce.ui.adapter.Coupon
import org.mozilla.rocket.content.ecommerce.ui.adapter.CouponAdapterDelegate
import org.mozilla.rocket.content.getActivityViewModel
import javax.inject.Inject

class CouponFragment : Fragment() {

    @Inject
    lateinit var shoppingViewModelCreator: Lazy<ShoppingViewModel>

    private lateinit var shoppingViewModel: ShoppingViewModel
    private lateinit var couponAdapter: DelegateAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        shoppingViewModel = getActivityViewModel(shoppingViewModelCreator)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_coupon, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initCoupons()
        bindListData()
        bindPageState()
    }

    private fun initCoupons() {
        couponAdapter = DelegateAdapter(
            AdapterDelegatesManager().apply {
                add(Coupon::class, R.layout.item_coupon, CouponAdapterDelegate(shoppingViewModel))
            }
        )
        val spaceWidth = resources.getDimensionPixelSize(R.dimen.card_space_width)
        val padding = resources.getDimensionPixelSize(R.dimen.card_padding)
        coupon_list.addItemDecoration(SpaceItemDecoration(spaceWidth, padding))
        coupon_list.adapter = couponAdapter
    }

    private fun bindListData() {
        shoppingViewModel.couponItems.observe(this@CouponFragment, Observer {
            couponAdapter.setData(it)
        })
    }

    private fun bindPageState() {
        shoppingViewModel.isDataLoading.observe(this@CouponFragment, Observer { state ->
            when (state) {
                is ShoppingViewModel.State.Idle -> showContentView()
                is ShoppingViewModel.State.Loading -> showLoadingView()
                is ShoppingViewModel.State.Error -> showErrorView()
            }
        })
    }

    private fun showLoadingView() {
        spinner.visibility = View.VISIBLE
    }

    private fun showContentView() {
        spinner.visibility = View.GONE
    }

    private fun showErrorView() {
        TODO("not implemented")
    }
}