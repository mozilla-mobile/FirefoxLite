package org.mozilla.rocket.content.ecommerce.ui.adapter

import android.view.View
import kotlinx.android.synthetic.main.item_product_category.*
import org.mozilla.focus.R
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.common.ui.VerticalTelemetryViewModel
import org.mozilla.rocket.content.common.ui.firstImpression
import org.mozilla.rocket.content.common.ui.monitorScrollImpression
import org.mozilla.rocket.content.ecommerce.StartSnapHelper
import org.mozilla.rocket.content.ecommerce.ui.DealViewModel
import org.mozilla.rocket.content.ecommerce.ui.HorizontalSpaceItemDecoration

class ProductCategoryAdapterDelegate(
    private val dealViewModel: DealViewModel,
    private val telemetryViewModel: VerticalTelemetryViewModel
) : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
        ProductCategoryViewHolder(view, dealViewModel, telemetryViewModel)
}

class ProductCategoryViewHolder(
    override val containerView: View,
    private val dealViewModel: DealViewModel,
    private val telemetryViewModel: VerticalTelemetryViewModel
) : DelegateAdapter.ViewHolder(containerView) {
    private var adapter = DelegateAdapter(
        AdapterDelegatesManager().apply {
            add(ProductItem::class, R.layout.item_product, ProductAdapterDelegate(dealViewModel))
        }
    )

    init {
        val spaceWidth = itemView.resources.getDimensionPixelSize(R.dimen.card_space_width)
        product_list.addItemDecoration(HorizontalSpaceItemDecoration(spaceWidth))
        product_list.adapter = this@ProductCategoryViewHolder.adapter
        val snapHelper = StartSnapHelper()
        snapHelper.attachToRecyclerView(product_list)
        product_list.monitorScrollImpression(telemetryViewModel)
    }

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        val productCategory = uiModel as ProductCategory
        category_title.text = if (productCategory.stringResourceId != 0) {
            category_title.context.getString(productCategory.stringResourceId)
        } else {
            productCategory.subcategoryName
        }

        adapter.setData(productCategory.items)

        if (!productCategory.items.isNullOrEmpty()) {
            product_list.firstImpression(
                telemetryViewModel,
                TelemetryWrapper.Extra_Value.SHOPPING_DEAL,
                productCategory.items[0].subCategoryId
            )
        }
    }
}

data class ProductCategory(
    val componentType: String,
    val subcategoryName: String,
    val stringResourceId: Int,
    val subcategoryId: Int,
    val items: List<ProductItem>
) : DelegateAdapter.UiModel()