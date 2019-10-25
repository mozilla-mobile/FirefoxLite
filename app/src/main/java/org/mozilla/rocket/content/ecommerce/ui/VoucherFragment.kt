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
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.common.ui.ContentTabActivity
import org.mozilla.rocket.content.ecommerce.ui.adapter.Voucher
import org.mozilla.rocket.content.ecommerce.ui.adapter.VoucherAdapterDelegate
import org.mozilla.rocket.content.getActivityViewModel
import javax.inject.Inject

class VoucherFragment : Fragment() {

    @Inject
    lateinit var voucherViewModelCreator: Lazy<VoucherViewModel>

    private lateinit var voucherViewModel: VoucherViewModel
    private lateinit var voucherAdapter: DelegateAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        voucherViewModel = getActivityViewModel(voucherViewModelCreator)
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
    }

    private fun bindListData() {
        voucherViewModel.voucherItems.observe(this@VoucherFragment, Observer {
            voucherAdapter.setData(it)
        })
    }

    private fun observeAction() {
        voucherViewModel.openVoucher.observe(this, Observer { linkUrl ->
            context?.let {
                startActivity(ContentTabActivity.getStartIntent(it, linkUrl))
            }
        })
    }
}