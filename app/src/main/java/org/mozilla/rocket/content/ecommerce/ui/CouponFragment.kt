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
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.common.ui.ContentTabActivity
import org.mozilla.rocket.content.common.ui.VerticalTelemetryViewModel
import org.mozilla.rocket.content.common.ui.firstImpression
import org.mozilla.rocket.content.common.ui.monitorScrollImpression
import org.mozilla.rocket.content.ecommerce.ui.adapter.Coupon
import org.mozilla.rocket.content.ecommerce.ui.adapter.CouponAdapterDelegate
import org.mozilla.rocket.content.getActivityViewModel
import javax.inject.Inject

class CouponFragment : Fragment() {

    @Inject
    lateinit var couponViewModelCreator: Lazy<CouponViewModel>

    @Inject
    lateinit var telemetryViewModelCreator: Lazy<VerticalTelemetryViewModel>

    private lateinit var couponViewModel: CouponViewModel
    private lateinit var telemetryViewModel: VerticalTelemetryViewModel
    private lateinit var couponAdapter: DelegateAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        couponViewModel = getActivityViewModel(couponViewModelCreator)
        telemetryViewModel = getActivityViewModel(telemetryViewModelCreator)
        couponViewModel.requestCoupons()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_coupon, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initCoupons()
        bindListData()
        bindPageState()
        observeAction()
        initNoResultView()
    }

    private fun initCoupons() {
        couponAdapter = DelegateAdapter(
            AdapterDelegatesManager().apply {
                add(Coupon::class, R.layout.item_coupon, CouponAdapterDelegate(couponViewModel))
            }
        )
        coupon_list.adapter = couponAdapter
        coupon_list.monitorScrollImpression(telemetryViewModel)
    }

    private fun bindListData() {
        couponViewModel.couponItems.observe(viewLifecycleOwner, Observer {
            couponAdapter.setData(it)
            telemetryViewModel.updateVersionId(TelemetryWrapper.Extra_Value.SHOPPING_COUPON, couponViewModel.versionId)

            if (!it.isNullOrEmpty() && it[0] is Coupon) {
                coupon_list.firstImpression(
                    telemetryViewModel,
                    TelemetryWrapper.Extra_Value.SHOPPING_COUPON,
                    (it[0] as Coupon).subCategoryId
                )
            }
        })
    }

    private fun bindPageState() {
        couponViewModel.isDataLoading.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is CouponViewModel.State.Idle -> showContentView()
                is CouponViewModel.State.Loading -> showLoadingView()
                is CouponViewModel.State.Error -> showErrorView()
            }
        })
    }

    private fun observeAction() {
        couponViewModel.openCoupon.observe(viewLifecycleOwner, Observer { action ->
            context?.let {
                startActivity(ContentTabActivity.getStartIntent(it, action.url, action.telemetryData))
            }
        })
    }

    private fun initNoResultView() {
        no_result_view.setButtonOnClickListener(View.OnClickListener {
            couponViewModel.onRetryButtonClicked()
        })
    }

    private fun showLoadingView() {
        spinner.visibility = View.VISIBLE
        coupon_list.visibility = View.GONE
        no_result_view.visibility = View.GONE
    }

    private fun showContentView() {
        spinner.visibility = View.GONE
        coupon_list.visibility = View.VISIBLE
        no_result_view.visibility = View.GONE
    }

    private fun showErrorView() {
        spinner.visibility = View.GONE
        coupon_list.visibility = View.GONE
        no_result_view.visibility = View.VISIBLE
    }
}