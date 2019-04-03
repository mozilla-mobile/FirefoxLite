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
import org.mozilla.rocket.content.data.Ticket

class TicketAdapter(private val listener: ContentAdapter.ContentPanelListener) : ListAdapter<Ticket, TicketViewHolder>(
        COMPARATOR
        ) {

    object COMPARATOR : DiffUtil.ItemCallback<Ticket>() {

        override fun areItemsTheSame(oldItem: Ticket, newItem: Ticket): Boolean {
            return oldItem.url == newItem.url
        }

        override fun areContentsTheSame(oldItem: Ticket, newItem: Ticket): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_tickets, parent, false)
        return TicketViewHolder(v)
    }

    override fun onBindViewHolder(holder: TicketViewHolder, position: Int) {
        val item = getItem(position) ?: return
        holder.bind(item, View.OnClickListener {
            listener.onItemClicked(item.url)
        })
    }
}

class TicketViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    var view: View? = null
    var image: ImageView? = null
    var name: TextView? = null

    init {
        view = itemView.findViewById(R.id.ticket_item)
        image = itemView.findViewById(R.id.ticket_category_image)
        name = itemView.findViewById(R.id.ticket_category_text)
    }

    fun bind(item: Ticket, listener: View.OnClickListener) {
        view?.setOnClickListener(listener)

        name?.text = item.name
        image?.setImageResource(item.image)
    }
}