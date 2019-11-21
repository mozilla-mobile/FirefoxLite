package org.mozilla.rocket.content.travel.domain

import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.travel.data.TravelRepository
import org.mozilla.rocket.content.travel.data.YoutubeApiEntity

class GetCityVideosUseCase(private val travelRepository: TravelRepository) {

    suspend operator fun invoke(name: String): Result<YoutubeApiEntity> =
            travelRepository.getCityVideos(name)
}