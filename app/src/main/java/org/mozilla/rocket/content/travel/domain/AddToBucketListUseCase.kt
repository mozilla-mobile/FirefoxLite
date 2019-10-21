package org.mozilla.rocket.content.travel.domain

import org.mozilla.rocket.content.travel.data.BucketListCity
import org.mozilla.rocket.content.travel.data.TravelRepository

class AddToBucketListUseCase(private val travelRepository: TravelRepository) {

    suspend operator fun invoke(id: String, name: String) {
        // TODO: query city imageUrl by id
        val imageUrl = ""
        travelRepository.addToBucketList(
                BucketListCity(
                    id,
                    imageUrl,
                    name
                )
        )
    }
}