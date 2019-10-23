package org.mozilla.rocket.content.travel.domain

import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.common.data.ApiEntity
import org.mozilla.rocket.content.travel.data.TravelRepository

class GetExploreListUseCase(private val travelRepository: TravelRepository) {

    suspend operator fun invoke(): Result<ApiEntity> =
            travelRepository.getExploreList()
}