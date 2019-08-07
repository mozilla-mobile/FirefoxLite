package org.mozilla.rocket.shopping.search.data

import org.mozilla.rocket.content.Result

class KeywordSuggestionRepository {

    @Suppress("UNUSED_PARAMETER")
    suspend fun fetchSuggestions(keyword: String): Result<List<String>> {
        return Result.Success(listOf("aa", "bbb", "cccc", "ddddd", "eeeeee"))
    }
}