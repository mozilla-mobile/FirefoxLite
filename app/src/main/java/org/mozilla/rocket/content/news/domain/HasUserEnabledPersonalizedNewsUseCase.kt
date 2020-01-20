package org.mozilla.rocket.content.news.domain

import org.mozilla.rocket.content.news.data.NewsSettingsRepository

class HasUserEnabledPersonalizedNewsUseCase(val repository: NewsSettingsRepository) {
    operator fun invoke(): Boolean {
        return repository.hasUserEnabledPersonalizedNews()
    }
}