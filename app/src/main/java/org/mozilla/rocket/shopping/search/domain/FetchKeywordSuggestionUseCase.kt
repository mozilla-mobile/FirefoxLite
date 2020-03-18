package org.mozilla.rocket.shopping.search.domain

import org.mozilla.rocket.content.Result
import org.mozilla.rocket.shopping.search.data.KeywordSuggestionRepository

class FetchKeywordSuggestionUseCase(val repository: KeywordSuggestionRepository) {

    suspend operator fun invoke(keyword: String): Result<List<String>> {
        return if (keyword.isEmpty()) {
            repository.fetchTrendingTerms()
        } else {
            repository.fetchSuggestions(keyword)
        }
    }
}