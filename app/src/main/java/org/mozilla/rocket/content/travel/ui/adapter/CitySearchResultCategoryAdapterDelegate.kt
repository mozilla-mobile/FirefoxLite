package org.mozilla.rocket.content.travel.ui.adapter

import android.view.View
import kotlinx.android.synthetic.main.item_city_search_result_category.*
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter

class CitySearchResultCategoryAdapterDelegate() : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder = CitySearchResultCategoryViewHolder(view)
}

class CitySearchResultCategoryViewHolder(override val containerView: View) : DelegateAdapter.ViewHolder(containerView) {
    override fun bind(uiModel: DelegateAdapter.UiModel) {
        uiModel as CitySearchResultCategoryUiModel
        icon.setImageResource(uiModel.imgResId)
        title.text = uiModel.title
    }
}

data class CitySearchResultCategoryUiModel(
    val imgResId: Int,
    val title: String
) : DelegateAdapter.UiModel()
