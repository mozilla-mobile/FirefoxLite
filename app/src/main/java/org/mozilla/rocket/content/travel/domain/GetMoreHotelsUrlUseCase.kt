package org.mozilla.rocket.content.travel.domain

import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.travel.data.TravelRepository

class GetMoreHotelsUrlUseCase(private val travelRepository: TravelRepository) {

    suspend operator fun invoke(name: String, id: String, type: String): Result<String> =
            travelRepository.getMoreHotelsUrl(name, id, type)
}