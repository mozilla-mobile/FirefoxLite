package org.mozilla.rocket.content.ecommerce.ui.adapter

import android.view.View
import kotlinx.android.synthetic.main.item_coupon_category.*
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.ecommerce.ui.ItemOffsetDecoration
import org.mozilla.rocket.content.ecommerce.ui.ShoppingViewModel

class CouponCategoryAdapterDelegate(private val shoppingViewModel: ShoppingViewModel) : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
        CouponCategoryViewHolder(view, shoppingViewModel)
}

class CouponCategoryViewHolder(
    override val containerView: View,
    private val shoppingViewModel: ShoppingViewModel
) : DelegateAdapter.ViewHolder(containerView) {
    private var adapter = DelegateAdapter(
        AdapterDelegatesManager().apply {
            add(Coupon::class, R.layout.item_coupon, CouponAdapterDelegate(shoppingViewModel))
        }
    )

    init {
        val spaceWidth = itemView.resources.getDimensionPixelSize(R.dimen.card_space_width)
        val padding = itemView.resources.getDimensionPixelSize(R.dimen.card_padding)
        val spanCount = itemView.resources.getInteger(R.integer.coupon_category_column)
        coupon_list.addItemDecoration(ItemOffsetDecoration(spaceWidth, padding, spanCount))
        coupon_list.adapter = this@CouponCategoryViewHolder.adapter
    }

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        val couponCategory = uiModel as CouponCategory
        category_title.text = couponCategory.title
        adapter.setData(couponCategory.couponList)
    }
}

data class CouponCategory(
    val id: String,
    val title: String,
    val couponList: List<Coupon>
) : DelegateAdapter.UiModel()