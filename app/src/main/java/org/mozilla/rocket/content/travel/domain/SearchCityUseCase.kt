package org.mozilla.rocket.content.travel.domain

import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.travel.data.BcAutocompleteApiEntity
import org.mozilla.rocket.content.travel.data.TravelRepository

class SearchCityUseCase(private val travelRepository: TravelRepository) {

    suspend operator fun invoke(keyword: String): Result<BcAutocompleteApiEntity> =
            travelRepository.searchCity(keyword)
}