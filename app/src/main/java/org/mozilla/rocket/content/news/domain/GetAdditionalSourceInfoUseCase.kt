package org.mozilla.rocket.content.news.domain

import org.mozilla.rocket.content.news.data.NewsSettingsRepository
import org.mozilla.rocket.content.news.data.NewsSourceInfo

class GetAdditionalSourceInfoUseCase(val repository: NewsSettingsRepository) {
    operator fun invoke(): NewsSourceInfo? {
        return repository.getAdditionalSourceInfo()
    }
}