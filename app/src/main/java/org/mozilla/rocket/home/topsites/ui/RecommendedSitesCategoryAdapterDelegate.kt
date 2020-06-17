package org.mozilla.rocket.home.topsites.ui

import android.view.View
import kotlinx.android.synthetic.main.item_recommended_sites_category.recommended_category
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter

class RecommendedSitesCategoryAdapterDelegate() : AdapterDelegate {

    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
        RecommendedSitesCategoryViewHolder(view)
}

class RecommendedSitesCategoryViewHolder(override val containerView: View) : DelegateAdapter.ViewHolder(containerView) {

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        val recommendedSitesUiCategory = uiModel as RecommendedSitesUiCategory
        recommended_category.text = recommendedSitesUiCategory.categoryName
    }
}

data class RecommendedSitesUiCategory(
    val categoryId: String,
    val categoryName: String
) : DelegateAdapter.UiModel()