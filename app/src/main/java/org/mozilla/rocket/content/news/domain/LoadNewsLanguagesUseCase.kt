package org.mozilla.rocket.content.news.domain

import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.isNotEmpty
import org.mozilla.rocket.content.news.data.NewsLanguage
import org.mozilla.rocket.content.news.data.NewsSettingsRepository
import javax.inject.Inject

class LoadNewsLanguagesUseCase @Inject constructor(private val repository: NewsSettingsRepository) {
    companion object {
        private const val DEFAULT_LANGUAGE_KEY = "English"
        private const val DEFAULT_LANGUAGE_CODE = "1"
        private val DEFAULT_LANGUAGE_LIST = listOf(
            NewsLanguage(DEFAULT_LANGUAGE_KEY, DEFAULT_LANGUAGE_CODE, DEFAULT_LANGUAGE_KEY)
        )
    }

    suspend operator fun invoke(): Result<List<NewsLanguage>> {
        val result = repository.getLanguagesV2()
        return if (result.isNotEmpty) {
            result
        } else {
            Result.Success(DEFAULT_LANGUAGE_LIST)
        }
    }
}
