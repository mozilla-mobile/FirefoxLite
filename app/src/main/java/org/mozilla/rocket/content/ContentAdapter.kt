package org.mozilla.rocket.content

import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import org.mozilla.focus.R
import org.mozilla.focus.fragment.PanelFragment
import org.mozilla.focus.fragment.PanelFragmentStatusListener
import org.mozilla.rocket.bhaskar.ItemPojo

class ContentAdapter(private val listener: ContentPanelListener) : ListAdapter<ItemPojo, NewsViewHolder>(
    COMPARATOR
) {

    init {

    }

    object COMPARATOR : DiffUtil.ItemCallback<ItemPojo>() {

        override fun areItemsTheSame(oldItem: ItemPojo, newItem: ItemPojo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ItemPojo, newItem: ItemPojo): Boolean {
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
            listener.onItemClicked(item.detailUrl)
            ContentPortalViewState.lasPos = position
        })
    }

    interface ContentPanelListener : PanelFragmentStatusListener {
        fun onItemClicked(url: String)

        fun onItemDeleted(item: ItemPojo?)

        fun onItemEdited(item: ItemPojo?)
    }
}

class NewsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    companion object {
        var requestOptions = RequestOptions().apply { transforms(CenterCrop(), RoundedCorners(16)) }
    }

    fun bind(item: ItemPojo, listener: View.OnClickListener) {
        itemView.findViewById<View>(R.id.news_item).setOnClickListener(listener)
        itemView.findViewById<TextView>(R.id.news_item_headline).text = item.title
        itemView.findViewById<TextView>(R.id.news_item_source).text = "bahaskar"
        itemView.findViewById<TextView>(R.id.news_item_time).text = "today"

        Glide.with(itemView.context)
            .load(item.coverPic)
            .apply(requestOptions)
            .into(itemView.findViewById(R.id.news_item_image))
    }
}