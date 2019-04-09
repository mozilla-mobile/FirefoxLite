package org.mozilla.rocket.content

import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import org.mozilla.focus.R
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.DrawableUtils
import org.mozilla.rocket.content.data.ShoppingLink

class ShoppingLinkAdapter(private val listener: ContentAdapter.ContentPanelListener) : ListAdapter<ShoppingLink, ShoppingLinkViewHolder>(
        COMPARATOR
        ) {

    object COMPARATOR : DiffUtil.ItemCallback<ShoppingLink>() {

        override fun areItemsTheSame(oldItem: ShoppingLink, newItem: ShoppingLink): Boolean {
            return oldItem.url == newItem.url
        }

        override fun areContentsTheSame(oldItem: ShoppingLink, newItem: ShoppingLink): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShoppingLinkViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_shoppinglink, parent, false)
        return ShoppingLinkViewHolder(v)
    }

    override fun onBindViewHolder(holder: ShoppingLinkViewHolder, position: Int) {
        val item = getItem(position) ?: return
        holder.bind(item, View.OnClickListener {
            TelemetryWrapper.clickOnEcItem(
                    pos = position.toString(),
                    source = item.source,
                    category = item.name)
            listener.onItemClicked(item.url)
        })
    }
}

class ShoppingLinkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    var view: View? = null
    var image: ImageView? = null
    var name: TextView? = null

    init {
        view = itemView.findViewById(R.id.shoppinglink_item)
        image = itemView.findViewById(R.id.shoppinglink_category_image)
        name = itemView.findViewById(R.id.shoppinglink_category_text)
    }

    fun bind(item: ShoppingLink, listener: View.OnClickListener) {
        view?.setOnClickListener(listener)

        name?.text = item.name
        DrawableUtils.getAndroidDrawable(itemView.context, item.image)?.let {
            image?.setImageDrawable(it)
        }
    }
}