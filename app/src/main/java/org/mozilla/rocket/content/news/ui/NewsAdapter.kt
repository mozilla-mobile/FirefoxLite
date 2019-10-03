package org.mozilla.rocket.content.news.ui

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import org.mozilla.focus.R
import org.mozilla.focus.glide.GlideApp
import org.mozilla.rocket.content.news.data.NewsItem
import org.mozilla.rocket.content.news.ui.NewsTabFragment.NewsListingEventListener

class NewsAdapter(private val listener: NewsListingEventListener) :
    ListAdapter<NewsItem, NewsViewHolder>(
        COMPARATOR
    ) {

    object COMPARATOR : DiffUtil.ItemCallback<NewsItem>() {

        override fun areItemsTheSame(oldItem: NewsItem, newItem: NewsItem): Boolean {
            return oldItem.title == newItem.title
        }

        override fun areContentsTheSame(oldItem: NewsItem, newItem: NewsItem): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_news, parent, false)
        return NewsViewHolder(v)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val item = getItem(position) ?: return
        holder.bind(item, View.OnClickListener {
            // TODO: Refine news click telemetry
            listener.onItemClicked(item.link)
        })
    }
}

class NewsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

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

        item.source.let {
            source?.text = it
        }

        time?.text = DateUtils.getRelativeTimeSpanString(
            item.publishTime, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
        )

        item.imageUrl?.let {
            if (image != null) {
                GlideApp.with(itemView.context)
                    .asBitmap()
                    .placeholder(R.drawable.placeholder)
                    .centerCrop()
                    .load(it)
                    .into(image)
            }
        }
    }
}