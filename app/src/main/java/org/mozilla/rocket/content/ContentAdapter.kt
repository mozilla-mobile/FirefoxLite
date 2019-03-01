package org.mozilla.rocket.content

import android.graphics.Bitmap
import android.graphics.Color
import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import org.mozilla.focus.R
import org.mozilla.focus.fragment.PanelFragment
import org.mozilla.focus.fragment.PanelFragmentStatusListener
import org.mozilla.focus.site.SiteItemViewHolder
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.DimenUtils
import org.mozilla.icon.FavIconUtils
import org.mozilla.rocket.bhaskar.ItemPojo

class ContentAdapter(private val listener: ContentPanelListener) : ListAdapter<ItemPojo, SiteItemViewHolder>(
    COMPARATOR
) {

    init {
        registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                if (itemCount == 0) {
                    listener.onStatus(PanelFragment.VIEW_TYPE_EMPTY)
                } else {
                    listener.onStatus(PanelFragment.VIEW_TYPE_NON_EMPTY)
                }
            }
        })
    }

    object COMPARATOR : DiffUtil.ItemCallback<ItemPojo>() {

        override fun areItemsTheSame(oldItem: ItemPojo, newItem: ItemPojo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ItemPojo, newItem: ItemPojo): Boolean {
            return oldItem == newItem
        }
    }

//    private var items: List<ItemPojo>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SiteItemViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_history_website, parent, false)
        return SiteItemViewHolder(v)
    }

    override fun onBindViewHolder(holder: SiteItemViewHolder, position: Int) {
        val item = getItem(position) ?: return
        val favIconUri = item.coverPic.substring(2, item.coverPic.length - 2)
        Glide.with(holder.imgFav.context)
            .asBitmap()
            .load(favIconUri)
            .into(object : SimpleTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>) {
                    if (DimenUtils.iconTooBlurry(holder.imgFav.resources, resource.width)) {
                        setImageViewWithDefaultBitmap(holder.imgFav, item.detailUrl)
                    } else {
                        holder.imgFav.setImageBitmap(resource)
                    }
                }
            })
        holder.rootView.tag = item.category
        holder.textMain.text = item.title
        holder.textSecondary.text = item.description
        holder.rootView.setOnClickListener { listener.onItemClicked(item.detailUrl) }
        val popupMenu = PopupMenu(holder.btnMore.context, holder.btnMore)
        popupMenu.setOnMenuItemClickListener { menuItem ->
            if (menuItem.itemId == R.id.remove) {
                listener.onItemDeleted(item)
            }
            if (menuItem.itemId == R.id.edit) {
                listener.onItemEdited(item)
            }
            false
        }
        popupMenu.inflate(R.menu.menu_bookmarks)
        holder.btnMore.setOnClickListener {
            popupMenu.show()
            TelemetryWrapper.showBookmarkContextMenu()
        }
    }
//
//    override fun getItemCount(): Int {
//        return if (items != null) items!!.size else 0
//    }

//    private fun getItem(index: Int): ItemPojo? {
//        return if (index >= 0 && items != null && items!!.size > index) {
//            items!![index]
//        } else {
//            null
//        }
//    }

    interface ContentPanelListener : PanelFragmentStatusListener {
        fun onItemClicked(url: String)

        fun onItemDeleted(item: ItemPojo?)

        fun onItemEdited(item: ItemPojo?)
    }

    private fun setImageViewWithDefaultBitmap(imageView: ImageView, url: String) {
        imageView.setImageBitmap(
            DimenUtils.getInitialBitmap(
                imageView.resources,
                FavIconUtils.getRepresentativeCharacter(url),
                Color.WHITE
            )
        )
    }
}