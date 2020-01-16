package org.mozilla.rocket.content.news.domain

import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.isNotEmpty
import org.mozilla.rocket.content.news.data.NewsLanguage
import org.mozilla.rocket.content.news.data.NewsSettingsRepository

class LoadRawNewsLanguagesUseCase(private val repository: NewsSettingsRepository) {

    suspend operator fun invoke(): Result<List<NewsLanguage>> {
        val defaultLanguage = repository.getDefaultLanguage().also { it.isSelected = true }
        val result = repository.getLanguages()
        if (result is Result.Success && result.isNotEmpty) {
            if (result.data == listOf(defaultLanguage)) {
                return Result.Error(Exception("No effective result"))
            }
        }
        return result
    }
}
