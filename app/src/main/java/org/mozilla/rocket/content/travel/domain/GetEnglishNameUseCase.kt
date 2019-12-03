package org.mozilla.rocket.content.travel.domain

import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.travel.data.TravelRepository

class GetEnglishNameUseCase(private val travelRepository: TravelRepository) {

    suspend operator fun invoke(id: String, type: String): Result<String> =
            travelRepository.getEnglishName(id, type)
}