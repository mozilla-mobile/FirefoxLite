package org.mozilla.rocket.content.ecommerce.adapter

import android.view.View
import kotlinx.android.synthetic.main.item_voucher.*
import org.mozilla.focus.utils.DrawableUtils
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.ecommerce.ShoppingViewModel

class VoucherAdapterDelegate(private val shoppingViewModel: ShoppingViewModel) : AdapterDelegate {

    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
            VoucherViewHolder(view, shoppingViewModel)
}

class VoucherViewHolder(
    override val containerView: View,
    private val shoppingViewModel: ShoppingViewModel
) : DelegateAdapter.ViewHolder(containerView) {

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        val voucherItem = uiModel as Voucher

        voucher_item.setOnClickListener {
            shoppingViewModel.onVoucherItemClicked(it.context, voucherItem)
        }

        voucher_category_text.text = voucherItem.name

        DrawableUtils.getAndroidDrawable(itemView.context, voucherItem.image)?.let {
            voucher_category_image.setImageDrawable(it)
        }
    }
}

object VoucherKey {
    const val KEY_NAME = "name"
    const val KEY_URL = "url"
    const val KEY_IMAGE = "img"
    const val KEY_SOURCE = "source"
}

data class Voucher(
    val url: String,
    val name: String,
    val image: String,
    val source: String
) : DelegateAdapter.UiModel()