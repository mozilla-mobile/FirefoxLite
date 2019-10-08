package org.mozilla.rocket.content.travel.domain

import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.travel.data.TravelRepository
import org.mozilla.rocket.content.travel.data.Wiki

class GetCityWikiUseCase(private val travelRepository: TravelRepository) {

    suspend operator fun invoke(name: String): Result<Wiki> =
            travelRepository.getCityWiki(name)
}