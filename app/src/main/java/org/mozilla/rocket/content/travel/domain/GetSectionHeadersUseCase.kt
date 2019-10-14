package org.mozilla.rocket.content.travel.domain

import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.travel.data.SectionType
import org.mozilla.rocket.content.travel.data.TravelRepository

class GetSectionHeadersUseCase(private val travelRepository: TravelRepository) {

    suspend operator fun invoke(name: String): Result<List<SectionType>> =
            travelRepository.getSectionHeaders(name)
}