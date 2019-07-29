package org.mozilla.rocket.vertical.games

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.rocket.vertical.games.GamesViewModel.Item

class BrowserGamesAdapter : RecyclerView.Adapter<BrowserGamesAdapter.BrowserGamesViewHolder>() {

    private var data = mutableListOf<Item>()

    fun setData(data: List<Item>) {
        this.data.clear()
        this.data.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BrowserGamesViewHolder {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: BrowserGamesViewHolder, position: Int) {
        holder.bind(data[position])
    }

    class BrowserGamesViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: Item) {
//            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }
}