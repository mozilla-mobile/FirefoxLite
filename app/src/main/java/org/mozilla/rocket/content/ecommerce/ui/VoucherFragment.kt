package org.mozilla.rocket.content.ecommerce.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import dagger.Lazy
import kotlinx.android.synthetic.main.fragment_voucher.*
import org.mozilla.focus.R
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.common.ui.ContentTabActivity
import org.mozilla.rocket.content.common.ui.VerticalTelemetryViewModel
import org.mozilla.rocket.content.common.ui.firstImpression
import org.mozilla.rocket.content.common.ui.monitorScrollImpression
import org.mozilla.rocket.content.ecommerce.ui.adapter.Voucher
import org.mozilla.rocket.content.ecommerce.ui.adapter.VoucherAdapterDelegate
import org.mozilla.rocket.content.getActivityViewModel
import javax.inject.Inject

class VoucherFragment : Fragment() {

    @Inject
    lateinit var voucherViewModelCreator: Lazy<VoucherViewModel>

    @Inject
    lateinit var telemetryViewModelCreator: Lazy<VerticalTelemetryViewModel>

    private lateinit var voucherViewModel: VoucherViewModel
    private lateinit var telemetryViewModel: VerticalTelemetryViewModel
    private lateinit var voucherAdapter: DelegateAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        voucherViewModel = getActivityViewModel(voucherViewModelCreator)
        telemetryViewModel = getActivityViewModel(telemetryViewModelCreator)
        voucherViewModel.requestVouchers()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_voucher, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initVouchers()
        bindListData()
        observeAction()
    }

    private fun initVouchers() {
        val spaceWidth = resources.getDimensionPixelSize(R.dimen.card_space_width)
        content_voucher_list.addItemDecoration(GridSpaceItemDecoration(spaceWidth, 2))
        voucherAdapter = DelegateAdapter(
            AdapterDelegatesManager().apply {
                add(Voucher::class, R.layout.item_voucher, VoucherAdapterDelegate(voucherViewModel))
            }
        )
        content_voucher_list.adapter = voucherAdapter
        content_voucher_list.monitorScrollImpression(telemetryViewModel)
    }

    private fun bindListData() {
        voucherViewModel.voucherItems.observe(viewLifecycleOwner, Observer {
            voucherAdapter.setData(it)
            telemetryViewModel.updateVersionId(TelemetryWrapper.Extra_Value.SHOPPING_VOUCHER, voucherViewModel.versionId)

            if (!it.isNullOrEmpty() && it[0] is Voucher) {
                content_voucher_list.firstImpression(
                    telemetryViewModel,
                    TelemetryWrapper.Extra_Value.SHOPPING_VOUCHER,
                    (it[0] as Voucher).subCategoryId
                )
            }
        })
    }

    private fun observeAction() {
        voucherViewModel.openVoucher.observe(viewLifecycleOwner, Observer { action ->
            context?.let {
                startActivity(ContentTabActivity.getStartIntent(it, action.url, action.telemetryData))
            }
        })
    }
}