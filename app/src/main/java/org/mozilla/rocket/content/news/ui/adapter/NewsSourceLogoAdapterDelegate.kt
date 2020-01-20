package org.mozilla.rocket.content.news.ui.adapter

import android.view.View
import kotlinx.android.synthetic.main.item_news_source_logo.*
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter

class NewsSourceLogoAdapterDelegate : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
        NewsSourceLogoViewHolder(view)
}

class NewsSourceLogoViewHolder(
    override val containerView: View
) : DelegateAdapter.ViewHolder(containerView) {

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        val newsUiModel = uiModel as NewsSourceLogoUiModel
        news_item_source_logo_image.setImageResource(newsUiModel.resourceId)
    }
}

data class NewsSourceLogoUiModel(
    val resourceId: Int
) : DelegateAdapter.UiModel()