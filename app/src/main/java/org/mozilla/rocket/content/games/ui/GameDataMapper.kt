package org.mozilla.rocket.content.games.ui

import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.common.adapter.Runway
import org.mozilla.rocket.content.common.adapter.RunwayItem
import org.mozilla.rocket.content.common.data.ApiEntity
import org.mozilla.rocket.content.common.data.ApiItem
import org.mozilla.rocket.content.games.ui.adapter.Game
import org.mozilla.rocket.content.games.ui.adapter.GameCategory

object GameDataMapper {

    private const val BANNER = "banner"

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
                    subcategory.items.map { gameItem -> toGameItem(gameItem) }
                )
            }
        }
    }

    private fun toRunwayItem(item: ApiItem): RunwayItem =
        RunwayItem(
            item.sourceName,
            item.image,
            item.destination,
            item.title,
            item.componentId
        )

    private fun toGameItem(item: ApiItem): Game =
        Game(
            item.sourceName,
            item.image,
            item.destination,
            item.title,
            item.description,
            item.componentId
        )
}