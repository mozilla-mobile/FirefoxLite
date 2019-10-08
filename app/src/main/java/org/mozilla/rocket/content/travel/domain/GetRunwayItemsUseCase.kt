package org.mozilla.rocket.content.travel.domain

import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.travel.data.RunwayItem
import org.mozilla.rocket.content.travel.data.TravelRepository

class GetRunwayItemsUseCase(private val travelRepository: TravelRepository) {

    suspend operator fun invoke(): Result<List<RunwayItem>> =
            travelRepository.getRunwayItems()
}