package org.mozilla.rocket.content.news.domain

import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.isNotEmpty
import org.mozilla.rocket.content.news.data.NewsLanguage
import org.mozilla.rocket.content.news.data.NewsSettingsRepository
import java.util.Locale

class LoadNewsLanguagesUseCase(private val repository: NewsSettingsRepository) {

    suspend operator fun invoke(): Result<List<NewsLanguage>> {
        var defaultLanguage = repository.getDefaultLanguage()
        val result = repository.getLanguages()
        if (result is Result.Success && result.isNotEmpty) {
            val supportLanguages = result.data
            if (supportLanguages.find { newsLanguage -> newsLanguage.isSelected } == null) {
                supportLanguages
                    .find { language -> Locale.getDefault().displayName.contains(language.name) }
                    ?.let { defaultLanguage = it }

                supportLanguages.forEach {
                    it.isSelected = (it.key == defaultLanguage.key)
                }
            }
        }
        return if (result.isNotEmpty) {
            result
        } else {
            Result.Success(listOf(repository.getDefaultLanguage()))
        }
    }
}
