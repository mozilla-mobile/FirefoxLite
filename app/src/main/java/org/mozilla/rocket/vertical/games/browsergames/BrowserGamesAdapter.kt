package org.mozilla.rocket.vertical.games.browsergames

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_carousel_banner.carousel_list
import kotlinx.android.synthetic.main.item_game_category.category_title
import kotlinx.android.synthetic.main.item_game_category.game_list
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.adapter.DelegateAdapter.UIModel
import org.mozilla.rocket.vertical.games.GamesViewModel

class BrowserGamesAdapter(
    private val delegatesManager: AdapterDelegatesManager
) : RecyclerView.Adapter<DelegateAdapter.ViewHolder>() {

    private var data = mutableListOf<UIModel>()

    fun setData(data: List<UIModel>) {
        this.data.clear()
        this.data.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DelegateAdapter.ViewHolder =
            delegatesManager.onCreateViewHolder(parent, viewType)

    override fun getItemViewType(position: Int): Int =
            delegatesManager.getItemViewType(data[position])

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: DelegateAdapter.ViewHolder, position: Int) {
        delegatesManager.onBindViewHolder(holder, data[position])
    }
}

class GameCategoryViewHolder(override val containerView: View, viewModel: GamesViewModel) : DelegateAdapter.ViewHolder(containerView) {
    private var adapter = GameCategoryAdapter(viewModel)

    init {
        game_list.apply {
            adapter = this@GameCategoryViewHolder.adapter
            layoutManager = LinearLayoutManager(containerView.context, RecyclerView.HORIZONTAL, false)
        }
    }

    override fun bind(UIModel: UIModel) {
        UIModel as GameCategory
        category_title.text = UIModel.title
        adapter.setData(UIModel.gameList)
    }
}

class CarouselBannerViewHolder(override val containerView: View, viewModel: GamesViewModel) : DelegateAdapter.ViewHolder(containerView) {
    private var adapter = CarouselBannerAdapter(viewModel)

    init {
        carousel_list.adapter = this@CarouselBannerViewHolder.adapter
    }

    override fun bind(UIModel: UIModel) {
        UIModel as CarouselBanner
        adapter.setData(UIModel.banners)
    }
}

data class CarouselBanner(val banners: List<CarouselBannerAdapter.BannerItem>) : UIModel()
data class GameCategory(val title: String, val gameList: List<GameCategoryAdapter.GameItem>) : UIModel()

class CarouselBannerAdapterDelegate(private val gamesViewModel: GamesViewModel) : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
            CarouselBannerViewHolder(view, gamesViewModel)
}

class GameCategoryAdapterDelegate(private val gamesViewModel: GamesViewModel) : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
            GameCategoryViewHolder(view, gamesViewModel)
}