package org.mozilla.rocket.content.ecommerce

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import dagger.Lazy
import kotlinx.android.synthetic.main.fragment_voucher.*
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.ecommerce.adapter.Voucher
import org.mozilla.rocket.content.ecommerce.adapter.VoucherAdapterDelegate
import org.mozilla.rocket.content.getActivityViewModel
import javax.inject.Inject

class VoucherFragment : Fragment() {

    @Inject
    lateinit var shoppingViewModelCreator: Lazy<ShoppingViewModel>

    private lateinit var shoppingViewModel: ShoppingViewModel
    private lateinit var voucherAdapter: DelegateAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        shoppingViewModel = getActivityViewModel(shoppingViewModelCreator)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_voucher, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initVouchers()
        bindListData()
    }

    private fun initVouchers() {
        voucherAdapter = DelegateAdapter(
            AdapterDelegatesManager().apply {
                add(Voucher::class, R.layout.item_voucher, VoucherAdapterDelegate(shoppingViewModel))
            }
        )
        content_voucher_list.apply {
            adapter = voucherAdapter
            layoutManager = GridLayoutManager(context, 2)
        }
    }

    private fun bindListData() {
        shoppingViewModel.voucherItems.observe(this@VoucherFragment, Observer {
            voucherAdapter.setData(it)
        })
    }
}