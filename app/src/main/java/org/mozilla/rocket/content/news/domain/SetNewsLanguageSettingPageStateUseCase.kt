package org.mozilla.rocket.content.news.domain

import org.mozilla.rocket.content.news.data.NewsSettingsRepository

class SetNewsLanguageSettingPageStateUseCase(val repository: NewsSettingsRepository) {
    operator fun invoke(enable: Boolean) {
        repository.setNewsLanguageSettingPageState(enable)
    }
}