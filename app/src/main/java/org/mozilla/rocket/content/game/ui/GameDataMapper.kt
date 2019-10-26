package org.mozilla.rocket.content.game.ui

import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.common.adapter.Runway
import org.mozilla.rocket.content.common.adapter.RunwayItem
import org.mozilla.rocket.content.common.data.ApiEntity
import org.mozilla.rocket.content.common.data.ApiItem
import org.mozilla.rocket.content.game.data.BANNER
import org.mozilla.rocket.content.game.data.MY_GAME
import org.mozilla.rocket.content.game.data.RECENT
import org.mozilla.rocket.content.game.ui.model.Game
import org.mozilla.rocket.content.game.ui.model.GameCategory
import org.mozilla.rocket.content.game.ui.model.GameType

object GameDataMapper {

    fun toGameUiModel(entity: ApiEntity): List<DelegateAdapter.UiModel> {
        return entity.subcategories.map { subcategory ->
            if (subcategory.componentType == BANNER) {
                Runway(
                    subcategory.componentType,
                    subcategory.subcategoryName,
                    subcategory.subcategoryId,
                    subcategory.items.map { gameItem -> toRunwayItem(gameItem) }
                )
            } else {
                GameCategory(
                    subcategory.componentType,
                    subcategory.subcategoryName,
                    subcategory.items.map { gameItem ->
                        when {
                            subcategory.componentType == RECENT -> toGameItem(gameItem, GameType.RecentlyPlayed)
                            subcategory.componentType == MY_GAME -> toGameItem(gameItem, GameType.MyGame)
                            else -> toGameItem(gameItem)
                        }
                    }
                )
            }
        }
    }

    private fun toRunwayItem(item: ApiItem): RunwayItem =
        RunwayItem(
            item.sourceName,
            item.categoryName,
            item.subCategoryId,
            item.image,
            item.destination,
            item.title,
            item.componentId
        )

    private fun toGameItem(item: ApiItem, gameType: GameType = GameType.Normal): Game =
        Game(
            item.sourceName,
            item.categoryName,
            item.subCategoryId,
            item.image,
            item.destination,
            item.title,
            item.description,
            item.componentId,
            gameType
        )

    fun toApiItem(game: Game): ApiItem =
        ApiItem(
            game.brand,
            game.category,
            game.subCategoryId,
            game.imageUrl,
            game.linkUrl,
            game.name,
            game.componentId,
            description = game.packageName
        )
}
