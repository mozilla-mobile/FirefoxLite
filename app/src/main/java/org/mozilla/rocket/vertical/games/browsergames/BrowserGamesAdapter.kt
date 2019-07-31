package org.mozilla.rocket.vertical.games.browsergames

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.collection.ArrayMap
import androidx.collection.SparseArrayCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_carousel_banner.carousel_list
import kotlinx.android.synthetic.main.item_game_category.category_title
import kotlinx.android.synthetic.main.item_game_category.game_list
import org.mozilla.rocket.vertical.games.GamesViewModel
import kotlin.reflect.KClass

class BrowserGamesAdapter(
    private val delegatesManager: AdapterDelegatesManager
) : RecyclerView.Adapter<BrowserGamesAdapter.ItemHolder>() {

    private var data = mutableListOf<Item>()

    fun setData(data: List<Item>) {
        this.data.clear()
        this.data.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder =
            delegatesManager.onCreateViewHolder(parent, viewType)

    override fun getItemViewType(position: Int): Int =
            delegatesManager.getItemViewType(data[position])

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
        delegatesManager.onBindViewHolder(holder, data[position])
    }

    sealed class ItemHolder(view: View) : RecyclerView.ViewHolder(view), LayoutContainer {

        abstract fun bind(item: Item)

        class GameCategoryViewHolder(override val containerView: View, viewModel: GamesViewModel) : ItemHolder(containerView) {
            private var adapter = GameCategoryAdapter(viewModel)

            init {
                game_list.apply {
                    adapter = this@GameCategoryViewHolder.adapter
                    layoutManager = LinearLayoutManager(containerView.context, RecyclerView.HORIZONTAL, false)
                }
            }

            override fun bind(item: Item) {
                item as Item.GameCategory
                category_title.text = item.title
                adapter.setData(item.gameList)
            }
        }

        class CarouselBannerViewHolder(override val containerView: View, viewModel: GamesViewModel) : ItemHolder(containerView) {
            private var adapter = CarouselBannerAdapter(viewModel)

            init {
                carousel_list.adapter = this@CarouselBannerViewHolder.adapter
            }

            override fun bind(item: Item) {
                item as Item.CarouselBanner
                adapter.setData(item.banners)
            }
        }
    }

    sealed class Item {
        data class CarouselBanner(val banners: List<CarouselBannerAdapter.BannerItem>) : Item()
        data class GameCategory(val title: String, val gameList: List<GameCategoryAdapter.GameItem>) : Item()
    }
}

interface AdapterDelegate {
    fun inflateView(parent: ViewGroup, layoutId: Int): View =
            LayoutInflater.from(parent.context).inflate(layoutId, parent, false)

    fun onCreateViewHolder(view: View): BrowserGamesAdapter.ItemHolder

    fun onBindViewHolder(item: BrowserGamesAdapter.Item, position: Int, holder: BrowserGamesAdapter.ItemHolder) {
        holder.bind(item)
    }
}

class CarouselBannerAdapterDelegate(private val gamesViewModel: GamesViewModel) : AdapterDelegate {
    override fun onCreateViewHolder(view: View): BrowserGamesAdapter.ItemHolder =
            BrowserGamesAdapter.ItemHolder.CarouselBannerViewHolder(view, gamesViewModel)
}

class GameCategoryAdapterDelegate(private val gamesViewModel: GamesViewModel) : AdapterDelegate {
    override fun onCreateViewHolder(view: View): BrowserGamesAdapter.ItemHolder =
            BrowserGamesAdapter.ItemHolder.GameCategoryViewHolder(view, gamesViewModel)
}

class AdapterDelegatesManager {
    private val typeDelegateMap = SparseArrayCompat<AdapterDelegate>()
    private val modelTypeMap = ArrayMap<KClass<out BrowserGamesAdapter.Item>, Int>()

    fun add(clazz: KClass<out BrowserGamesAdapter.Item>, layoutId: Int, delegate: AdapterDelegate) {
        typeDelegateMap.put(layoutId, delegate)
        modelTypeMap[clazz] = layoutId
    }

    fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BrowserGamesAdapter.ItemHolder {
        val delegate = typeDelegateMap[viewType]
        requireNotNull(delegate) { "Cannot find delegate with viewType: $viewType" }
        val view = delegate.inflateView(parent, viewType)

        return delegate.onCreateViewHolder(view)
    }

    fun getItemViewType(item: BrowserGamesAdapter.Item): Int =
            modelTypeMap[item::class] ?: error("Cannot find viewType with class: ${item.javaClass}")

    fun onBindViewHolder(holder: BrowserGamesAdapter.ItemHolder, item: BrowserGamesAdapter.Item) {
        holder.bind(item)
    }
}