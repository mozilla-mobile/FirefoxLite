package org.mozilla.rocket.content.travel.domain

import org.mozilla.rocket.content.travel.data.TravelRepository

class RemoveFromBucketListUseCase(private val travelRepository: TravelRepository) {

    suspend operator fun invoke(id: String) {
        travelRepository.removeFromBucketList(id)
    }
}