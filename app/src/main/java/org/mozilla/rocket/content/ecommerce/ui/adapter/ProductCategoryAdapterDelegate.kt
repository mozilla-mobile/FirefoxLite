package org.mozilla.rocket.content.ecommerce.ui.adapter

import android.view.View
import kotlinx.android.synthetic.main.item_product_category.*
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.ecommerce.StartSnapHelper
import org.mozilla.rocket.content.ecommerce.ui.HorizontalSpaceItemDecoration
import org.mozilla.rocket.content.ecommerce.ui.ShoppingViewModel

class ProductCategoryAdapterDelegate(private val shoppingViewModel: ShoppingViewModel) : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
        ProductCategoryViewHolder(view, shoppingViewModel)
}

class ProductCategoryViewHolder(
    override val containerView: View,
    private val shoppingViewModel: ShoppingViewModel
) : DelegateAdapter.ViewHolder(containerView) {
    private var adapter = DelegateAdapter(
        AdapterDelegatesManager().apply {
            add(ProductItem::class, R.layout.item_product, ProductAdapterDelegate(shoppingViewModel))
        }
    )

    init {
        val spaceWidth = itemView.resources.getDimensionPixelSize(R.dimen.card_space_width)
        val padding = itemView.resources.getDimensionPixelSize(R.dimen.card_padding)
        product_list.addItemDecoration(HorizontalSpaceItemDecoration(spaceWidth, padding))
        product_list.adapter = this@ProductCategoryViewHolder.adapter
        val snapHelper = StartSnapHelper()
        snapHelper.attachToRecyclerView(product_list)
    }

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        val productCategory = uiModel as ProductCategory
        category_title.text = productCategory.title
        adapter.setData(productCategory.productList)
    }
}

data class ProductCategory(
    val id: String,
    val title: String,
    val productList: List<ProductItem>
) : DelegateAdapter.UiModel()