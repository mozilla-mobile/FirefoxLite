package org.mozilla.rocket.content.news

import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import org.mozilla.focus.R
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.lite.partner.NewsItem
import org.mozilla.rocket.content.portal.ContentPortalListener

class NewsAdapter<T : NewsItem>(private val listener: ContentPortalListener) :
        ListAdapter<NewsItem, NewsViewHolder<T>>(
            COMPARATOR
        ) {

    object COMPARATOR : DiffUtil.ItemCallback<NewsItem>() {

        override fun areItemsTheSame(oldItem: NewsItem, newItem: NewsItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: NewsItem, newItem: NewsItem): Boolean {
            return oldItem.id == newItem.id
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder<T> {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_news, parent, false)
        return NewsViewHolder(v)
    }

    override fun onBindViewHolder(holder: NewsViewHolder<T>, position: Int) {
        val item = getItem(position) ?: return
        holder.bind(item, View.OnClickListener {
                TelemetryWrapper.clickOnNewsItem(
                        pos = position.toString(),
                        feed = item.source,
                        source = item.partner,
                        category = item.category,
                        subCategory = item.subcategory)
                listener.onItemClicked(item.newsUrl)
        })
    }
}

class NewsViewHolder<T : NewsItem>(itemView: View) : RecyclerView.ViewHolder(itemView) {

    companion object {
        var requestOptions =
                RequestOptions().apply { transforms(CenterCrop(), RoundedCorners(16)) }
    }

    var view: View? = null
    var headline: TextView? = null
    var source: TextView? = null
    var time: TextView? = null
    var image: ImageView? = null

    init {
        view = itemView.findViewById(R.id.news_item)
        headline = itemView.findViewById(R.id.news_item_headline)
        source = itemView.findViewById(R.id.news_item_source)
        time = itemView.findViewById(R.id.news_item_time)
        image = itemView.findViewById(R.id.news_item_image)
    }

    fun bind(item: NewsItem, listener: View.OnClickListener) {
        view?.setOnClickListener(listener)

        headline?.text = item.title

        item.partner.let {
            source?.text = it
        }

        time?.text = DateUtils.getRelativeTimeSpanString(
                item.time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
        )

        item.imageUrl?.let {
            if (image != null) {
                    Glide.with(itemView.context).load(it).apply(requestOptions)
                    .into(image)
            }
        }
    }
}