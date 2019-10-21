package org.mozilla.rocket.content.travel.domain

import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.travel.data.BucketListCity
import org.mozilla.rocket.content.travel.data.TravelRepository

class GetBucketListUseCase(private val travelRepository: TravelRepository) {

    suspend operator fun invoke(): Result<List<BucketListCity>> =
            travelRepository.getBucketList()
}