package org.mozilla.rocket.content.news.domain

import org.mozilla.rocket.content.news.data.NewsSettingsRepository

class SetUserEnabledPersonalizedNewsUseCase(val repository: NewsSettingsRepository) {
    operator fun invoke(enable: Boolean) {
        repository.setUserEnabledPersonalizedNews(enable)
    }
}