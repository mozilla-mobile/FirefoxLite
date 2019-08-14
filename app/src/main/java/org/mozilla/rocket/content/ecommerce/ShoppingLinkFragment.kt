package org.mozilla.rocket.content.ecommerce

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.android.synthetic.main.content_tab_shoppinglink.*
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.ecommerce.adapter.*

class ShoppingLinkFragment : Fragment() {

    private lateinit var shoppingViewModel: ShoppingViewModel
    private lateinit var shoppingLinkAdapter: DelegateAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shoppingViewModel = ViewModelProviders.of(requireActivity(), ShoppingViewModelFactory.INSTANCE).get(ShoppingViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.content_tab_shoppinglink, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initShoppingLinks()
        bindListData()
    }

    private fun initShoppingLinks() {
        shoppingLinkAdapter = DelegateAdapter(
            AdapterDelegatesManager().apply {
                add(ShoppingLink::class, R.layout.item_shoppinglink, ShoppingLinkAdapterDelegate(shoppingViewModel))
            }
        )
        content_shoppinglink_list.apply {
            adapter = shoppingLinkAdapter
            layoutManager = GridLayoutManager(context, 2)
        }
    }

    private fun bindListData() {
        shoppingViewModel.shoppingLinkItems.observe(this@ShoppingLinkFragment, Observer {
            shoppingLinkAdapter.setData(it)
        })
    }
}