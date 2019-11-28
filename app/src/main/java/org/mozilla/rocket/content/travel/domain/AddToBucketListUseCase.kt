package org.mozilla.rocket.content.travel.domain

import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.travel.data.BucketListCity
import org.mozilla.rocket.content.travel.data.TravelRepository

class AddToBucketListUseCase(private val travelRepository: TravelRepository) {

    suspend operator fun invoke(id: String, name: String, type: String, nameInEnglish: String, countryCode: String) {
        travelRepository.addToBucketList(BucketListCity(id, getImageUrl(id), name, type, nameInEnglish, countryCode))
    }

    private suspend fun getImageUrl(id: String): String {
        val exploreCityResult = travelRepository.getExploreList()
        if (exploreCityResult is Result.Success) {
            val apiEntity = exploreCityResult.data
            for (category in apiEntity.subcategories) {
                if (category.componentType == BANNER) {
                    continue
                }
                for (item in category.items) {
                    if (item.description == id) {
                        return item.image
                    }
                }
            }
        }
        return ""
    }

    companion object {
        private const val BANNER = "banner"
    }
}
