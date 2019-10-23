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
import org.mozilla.focus.R
import org.mozilla.focus.glide.GlideApp
import org.mozilla.rocket.content.news.data.NewsItem

class NewsAdapter(private val category: String, private val newsViewModel: NewsViewModel) :
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
            newsViewModel.onNewsItemClicked(category, item)
        })
    }
}

class NewsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

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

        time?.text =
            if (item.publishTime == Long.MIN_VALUE) {
                ""
            } else {
                DateUtils.getRelativeTimeSpanString(
                    item.publishTime, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
                )
            }

        if (item.imageUrl != null) {
            image?.run {
                visibility = View.VISIBLE
                GlideApp.with(itemView.context)
                        .asBitmap()
                        .placeholder(R.drawable.placeholder)
                        .centerCrop()
                        .load(item.imageUrl)
                        .into(this)
            }
        } else {
            image?.visibility = View.GONE
        }
    }
}
