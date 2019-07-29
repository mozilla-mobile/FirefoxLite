package org.mozilla.rocket.vertical.games.item

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.focus.R
import org.mozilla.focus.glide.GlideApp
import org.mozilla.rocket.vertical.games.GamesViewModel
import org.mozilla.rocket.vertical.games.GamesViewModel.GameItem

class GameCategoryAdapter(
    private val gamesViewModel: GamesViewModel
) : RecyclerView.Adapter<GameCategoryAdapter.GameViewHolder>() {

    private var data = mutableListOf<GameItem>()

    fun setData(data: List<GameItem>) {
        this.data.clear()
        this.data.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_game, parent, false)
        return GameViewHolder(view).apply {
            setOnItemClickListener { gamesViewModel.onGameItemClicked(it) }
        }
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        holder.bind(data[position])
    }

    class GameViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // TODO: use kotlinx
        val name: TextView = itemView.findViewById(R.id.name)
        val image: ImageView = itemView.findViewById(R.id.image)

        private var onItemClickListener: ((GameItem) -> Unit)? = null

        fun bind(gameItem: GameItem) {
            this.name.text = gameItem.name
            GlideApp.with(itemView.context)
                    .asBitmap()
                    .placeholder(R.drawable.placeholder)
                    .fitCenter()
                    .load(gameItem.imageUrl)
                    .into(image)

            itemView.setOnClickListener { onItemClickListener?.invoke(gameItem) }
        }

        fun setOnItemClickListener(listener: (GameItem) -> Unit) {
            onItemClickListener = listener
        }
    }
}