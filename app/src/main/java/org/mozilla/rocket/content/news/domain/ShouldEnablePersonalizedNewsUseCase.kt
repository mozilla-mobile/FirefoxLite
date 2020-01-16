package org.mozilla.rocket.content.news.domain

import org.mozilla.rocket.content.news.data.NewsSettingsRepository

class ShouldEnablePersonalizedNewsUseCase(val repository: NewsSettingsRepository) {
    operator fun invoke(): Boolean {
        return repository.shouldEnablePersonalizedNews()
    }
}