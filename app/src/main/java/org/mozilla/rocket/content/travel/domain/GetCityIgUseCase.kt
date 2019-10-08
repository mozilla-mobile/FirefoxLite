package org.mozilla.rocket.content.travel.domain

import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.travel.data.Ig
import org.mozilla.rocket.content.travel.data.TravelRepository

class GetCityIgUseCase(private val travelRepository: TravelRepository) {

    suspend operator fun invoke(name: String): Result<Ig> =
            travelRepository.getCityIg(name)
}