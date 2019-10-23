package org.mozilla.rocket.content.travel.ui.adapter

import android.view.View
import kotlinx.android.synthetic.main.item_city_category.*
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.ecommerce.StartSnapHelper
import org.mozilla.rocket.content.ecommerce.ui.HorizontalSpaceItemDecoration
import org.mozilla.rocket.content.travel.ui.TravelExploreViewModel

class CityCategoryAdapterDelegate(private val travelExploreViewModel: TravelExploreViewModel) : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
        CityCategoryViewHolder(view, travelExploreViewModel)
}

class CityCategoryViewHolder(
    override val containerView: View,
    private val travelExploreViewModel: TravelExploreViewModel
) : DelegateAdapter.ViewHolder(containerView) {
    private var adapter = DelegateAdapter(
        AdapterDelegatesManager().apply {
            add(CityUiModel::class, R.layout.item_city, CityAdapterDelegate(travelExploreViewModel))
        }
    )

    init {
        val spaceWidth = itemView.resources.getDimensionPixelSize(R.dimen.card_space_width)
        city_list.addItemDecoration(HorizontalSpaceItemDecoration(spaceWidth))
        city_list.adapter = this@CityCategoryViewHolder.adapter
        val snapHelper = StartSnapHelper()
        snapHelper.attachToRecyclerView(city_list)
    }

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        val cityCategory = uiModel as CityCategoryUiModel
        category_title.text = cityCategory.subcategoryName
        adapter.setData(cityCategory.cityList)
    }
}

data class CityCategoryUiModel(
    val componentType: String,
    val subcategoryName: String,
    val subcategoryId: Int,
    val cityList: List<CityUiModel>
) : DelegateAdapter.UiModel()