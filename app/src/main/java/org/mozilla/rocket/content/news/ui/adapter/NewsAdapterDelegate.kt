package org.mozilla.rocket.content.news.ui.adapter

import android.text.format.DateUtils
import android.view.View
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.item_news.*
import org.mozilla.focus.R
import org.mozilla.focus.glide.GlideApp
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.news.ui.NewsViewModel

class NewsAdapterDelegate(private val category: String, private val newsViewModel: NewsViewModel) : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
        NewsViewHolder(view, category, newsViewModel)
}

class NewsViewHolder(
    override val containerView: View,
    private val category: String,
    private val newsViewModel: NewsViewModel
) : DelegateAdapter.ViewHolder(containerView) {

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        val newsUiModel = uiModel as NewsUiModel

        itemView.setOnClickListener { newsViewModel.onNewsItemClicked(category, newsUiModel) }

        news_item_headline.text = newsUiModel.title
        news_item_source.text = newsUiModel.source
        news_item_time.text =
            if (newsUiModel.publishTime == Long.MIN_VALUE) {
                ""
            } else {
                DateUtils.getRelativeTimeSpanString(
                    newsUiModel.publishTime, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
                )
            }

        GlideApp.with(itemView.context)
            .asBitmap()
            .placeholder(R.drawable.placeholder)
            .centerCrop()
            .load(newsUiModel.imageUrl)
            .into(news_item_image)

        news_item_image.isVisible = newsUiModel.imageUrl != null
    }
}

data class NewsUiModel(
    val title: String,
    val link: String,
    val source: String,
    val imageUrl: String?,
    val publishTime: Long,
    val componentId: String,
    val feed: String,
    val subCategoryId: String,
    val trackingUrl: String = "",
    val trackingId: String = "",
    val trackingData: String = "",
    val attributionUrl: String = ""
) : DelegateAdapter.UiModel()