package org.mozilla.rocket.content.ecommerce

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.content_tab_coupon.*
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.ecommerce.adapter.Coupon
import org.mozilla.rocket.content.ecommerce.adapter.CouponAdapterDelegate
import org.mozilla.rocket.content.ecommerce.adapter.CouponRunway
import org.mozilla.rocket.content.ecommerce.adapter.CouponRunwayAdapterDelegate

class CouponFragment : Fragment() {

    private lateinit var shoppingViewModel: ShoppingViewModel
    private lateinit var couponAdapter: DelegateAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shoppingViewModel = ViewModelProviders.of(requireActivity(), ShoppingViewModelFactory.INSTANCE).get(ShoppingViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.content_tab_coupon, container, false)
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
                add(CouponRunway::class, R.layout.item_runway_list, CouponRunwayAdapterDelegate(shoppingViewModel))
                add(Coupon::class, R.layout.item_coupon, CouponAdapterDelegate(shoppingViewModel))
            }
        )
        content_coupons.apply {
            adapter = couponAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun bindListData() {
        shoppingViewModel.couponItems.observe(this@CouponFragment, Observer {
            couponAdapter.setData(it)
        })
    }

    private fun bindPageState() {
        shoppingViewModel.isDataLoading.observe(this@CouponFragment, Observer {
            spinner.visibility = if (it) View.VISIBLE else View.GONE
        })
    }
}