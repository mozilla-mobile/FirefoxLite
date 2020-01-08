package org.mozilla.rocket.content.travel.domain

import org.mozilla.rocket.content.travel.data.TravelSearchSettingRepository

class SetTravelSearchOptionPromptHasShownUseCase(val repository: TravelSearchSettingRepository) {
    operator fun invoke() {
        return repository.setSearchOptionPromptHasShown()
    }
}