package org.mozilla.rocket.content.travel.domain

import org.mozilla.rocket.content.travel.data.TravelSearchSettingRepository

class ShouldShowTravelSearchOptionPromptUseCase(val repository: TravelSearchSettingRepository) {
    operator fun invoke(): Boolean {
        return repository.shouldShowSearchOptionPrompt()
    }
}