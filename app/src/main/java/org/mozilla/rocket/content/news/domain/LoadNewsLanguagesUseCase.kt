package org.mozilla.rocket.content.news.domain

import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.isNotEmpty
import org.mozilla.rocket.content.news.data.NewsLanguage
import org.mozilla.rocket.content.news.data.NewsSettingsRepositoryProvider
import java.util.Locale

class LoadNewsLanguagesUseCase(repositoryProvider: NewsSettingsRepositoryProvider) {

    val repository = repositoryProvider.provideNewsSettingsRepository()

    suspend operator fun invoke(): Result<List<NewsLanguage>> {
        var defaultLanguage = DEFAULT_LANGUAGE
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
            Result.Success(listOf(DEFAULT_LANGUAGE))
        }
    }

    companion object {
        private const val DEFAULT_LANGUAGE_KEY = "English"
        private const val DEFAULT_LANGUAGE_CODE = "1"
        val DEFAULT_LANGUAGE = NewsLanguage(DEFAULT_LANGUAGE_KEY, DEFAULT_LANGUAGE_CODE, DEFAULT_LANGUAGE_KEY)
    }
}
