package org.mozilla.rocket.content.travel.domain

import org.mozilla.rocket.content.travel.data.TravelSearchSettingRepository
import org.mozilla.rocket.content.travel.data.TravelSearchSettingRepository.TravelSearchSetting

class ShouldShowChangeTravelSearchSettingUseCase(val repository: TravelSearchSettingRepository) {
    operator fun invoke(): Boolean {
        return repository.getSearchSetting() == TravelSearchSetting.Default
    }
}