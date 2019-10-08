package org.mozilla.rocket.content.travel.domain

import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.travel.data.CityCategory
import org.mozilla.rocket.content.travel.data.TravelRepository

class GetCityCategoriesUseCase(private val travelRepository: TravelRepository) {

    suspend operator fun invoke(): Result<List<CityCategory>> =
            travelRepository.getCityCategories()
}