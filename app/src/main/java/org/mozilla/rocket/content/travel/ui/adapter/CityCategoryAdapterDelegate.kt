package org.mozilla.rocket.content.travel.ui.adapter

import android.view.View
import kotlinx.android.synthetic.main.item_city_category.*
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.ecommerce.StartSnapHelper
import org.mozilla.rocket.content.ecommerce.ui.HorizontalSpaceItemDecoration

class CityCategoryAdapterDelegate() : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
        CityCategoryViewHolder(view)
}

class CityCategoryViewHolder(
    override val containerView: View
) : DelegateAdapter.ViewHolder(containerView) {
    private var adapter = DelegateAdapter(
        AdapterDelegatesManager().apply {
            add(CityItem::class, R.layout.item_city, CityAdapterDelegate())
        }
    )

    init {
        val spaceWidth = itemView.resources.getDimensionPixelSize(R.dimen.card_space_width)
        city_list.addItemDecoration(HorizontalSpaceItemDecoration(spaceWidth))
        city_list.adapter = this@CityCategoryViewHolder.adapter
        //kinse check what is this
        val snapHelper = StartSnapHelper()
        snapHelper.attachToRecyclerView(city_list)
    }

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        val cityCategory = uiModel as CityCategory
        category_title.text = cityCategory.categoryName
        adapter.setData(cityCategory.items)
    }
}

data class CityCategory(
    val categoryName: String,
    val items: List<CityItem>
) : DelegateAdapter.UiModel()