package org.mozilla.rocket.content.ecommerce

import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import org.mozilla.focus.R
import org.mozilla.focus.navigation.ScreenNavigator
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.DrawableUtils
import org.mozilla.rocket.content.ecommerce.data.ShoppingLink

class ShoppingLinkAdapter : ListAdapter<ShoppingLink, ShoppingLinkViewHolder>(
    COMPARATOR
        ) {

    companion object {
        const val ITEM_VIEW_TYPE_CONTENT = 0
        const val ITEM_VIEW_TYPE_FOOTER = 1
    }
    object COMPARATOR : DiffUtil.ItemCallback<ShoppingLink>() {

        override fun areItemsTheSame(oldItem: ShoppingLink, newItem: ShoppingLink): Boolean {
            return oldItem.url == newItem.url
        }

        override fun areContentsTheSame(oldItem: ShoppingLink, newItem: ShoppingLink): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShoppingLinkViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == ITEM_VIEW_TYPE_FOOTER) {
            val v = inflater.inflate(R.layout.item_shoppinglink_footer, parent, false)
            ShoppingLinkViewHolder(v)
        } else {
            val v = inflater.inflate(R.layout.item_shoppinglink, parent, false)
            ShoppingLinkViewHolder(v)
        }
    }

    override fun onBindViewHolder(holder: ShoppingLinkViewHolder, position: Int) {
        val item = getItem(position) ?: return
        holder.bind(item, position)
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == itemCount - 1) {
            ITEM_VIEW_TYPE_FOOTER
        } else {
            ITEM_VIEW_TYPE_CONTENT
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        val layoutManager = recyclerView.layoutManager
        if (layoutManager is GridLayoutManager) {
            val cacheSpanCount = layoutManager.spanCount
            layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(pos: Int): Int {
                    return if (getItemViewType(pos) == ITEM_VIEW_TYPE_FOOTER) {
                        cacheSpanCount
                    } else {
                        1
                    }
                }
            }
        }
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

    fun bind(item: ShoppingLink, position: Int) {
        view?.setOnClickListener {
            ScreenNavigator.get(it.context).showBrowserScreen(item.url, true, false)
            TelemetryWrapper.clickOnEcItem(
                pos = position.toString(),
                source = item.source,
                category = item.name
            )
        }

        name?.text = item.name
        DrawableUtils.getAndroidDrawable(itemView.context, item.image)?.let {
            image?.setImageDrawable(it)
        }
    }
}