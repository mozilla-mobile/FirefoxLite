package org.mozilla.rocket.content.travel.domain

import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.travel.data.Hotel
import org.mozilla.rocket.content.travel.data.TravelRepository

class GetCityHotelsUseCase(private val travelRepository: TravelRepository) {

    suspend operator fun invoke(name: String): Result<List<Hotel>> =
            travelRepository.getCityHotels(name)
}