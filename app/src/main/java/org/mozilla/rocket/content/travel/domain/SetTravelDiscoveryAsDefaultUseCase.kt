package org.mozilla.rocket.content.travel.domain

import org.mozilla.rocket.content.travel.data.TravelSearchSettingRepository
import org.mozilla.rocket.content.travel.data.TravelSearchSettingRepository.TravelSearchSetting

class SetTravelDiscoveryAsDefaultUseCase(val repository: TravelSearchSettingRepository) {
    operator fun invoke(setTravelDiscoveryAsDefault: Boolean) {
        if (setTravelDiscoveryAsDefault) {
            repository.setAsDefaultSearch(TravelSearchSetting.FirefoxLite)
        } else {
            repository.setAsDefaultSearch(TravelSearchSetting.Google)
        }
    }
}