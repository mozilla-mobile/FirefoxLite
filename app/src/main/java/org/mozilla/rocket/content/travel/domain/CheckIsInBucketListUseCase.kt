package org.mozilla.rocket.content.travel.domain

import org.mozilla.rocket.content.travel.data.TravelRepository

class CheckIsInBucketListUseCase(private val travelRepository: TravelRepository) {

    suspend operator fun invoke(id: String): Boolean =
            travelRepository.isInBucketList(id)
}