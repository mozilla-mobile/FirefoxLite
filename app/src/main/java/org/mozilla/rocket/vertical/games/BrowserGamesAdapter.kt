package org.mozilla.rocket.vertical.games

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_carousel_banner.carousel_list
import kotlinx.android.synthetic.main.item_game_category.category_title
import kotlinx.android.synthetic.main.item_game_category.game_list
import org.mozilla.focus.R
import org.mozilla.rocket.vertical.games.item.CarouselBannerAdapter
import org.mozilla.rocket.vertical.games.item.GameCategoryAdapter

class BrowserGamesAdapter(
    private val gamesViewModel: GamesViewModel
) : RecyclerView.Adapter<BrowserGamesAdapter.ItemHolder>() {

    private var data = mutableListOf<GamesViewModel.Item>()

    fun setData(data: List<GamesViewModel.Item>) {
        this.data.clear()
        this.data.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            ITEM_TYPE_CAROUSEL_BANNER -> {
                val view = inflater.inflate(R.layout.item_carousel_banner, parent, false)
                ItemHolder.CarouselBannerViewHolder(view, gamesViewModel)
            }
            ITEM_TYPE_GAME_CATEGORY -> {
                val view = inflater.inflate(R.layout.item_game_category, parent, false)
                ItemHolder.GameCategoryViewHolder(view, gamesViewModel)
            }
            else -> error("invalid viewType")
        }
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
        holder.bind(data[position])
    }

    override fun getItemViewType(position: Int): Int = when (data[position]) {
        is GamesViewModel.Item.CarouselBanner -> ITEM_TYPE_CAROUSEL_BANNER
        is GamesViewModel.Item.GameCategory -> ITEM_TYPE_GAME_CATEGORY
    }

    sealed class ItemHolder(view: View) : RecyclerView.ViewHolder(view), LayoutContainer {

        abstract fun bind(item: GamesViewModel.Item)

        class GameCategoryViewHolder(override val containerView: View, viewModel: GamesViewModel) : ItemHolder(containerView) {
            private var adapter = GameCategoryAdapter(viewModel)

            init {
                game_list.apply {
                    adapter = this@GameCategoryViewHolder.adapter
                    layoutManager = LinearLayoutManager(containerView.context, RecyclerView.HORIZONTAL, false)
                }
            }

            override fun bind(item: GamesViewModel.Item) {
                item as GamesViewModel.Item.GameCategory
                category_title.text = item.title
                adapter.setData(item.gameList)
            }
        }

        class CarouselBannerViewHolder(override val containerView: View, viewModel: GamesViewModel) : ItemHolder(containerView) {
            private var adapter = CarouselBannerAdapter(viewModel)

            init {
                carousel_list.adapter = this@CarouselBannerViewHolder.adapter
            }

            override fun bind(item: GamesViewModel.Item) {
                item as GamesViewModel.Item.CarouselBanner
                adapter.setData(item.banners)
            }
        }
    }

    companion object {
        const val ITEM_TYPE_CAROUSEL_BANNER = 1
        const val ITEM_TYPE_GAME_CATEGORY = 2
    }
}