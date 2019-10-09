package org.mozilla.rocket.shopping.search.data

import android.content.Context
import android.text.TextUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mozilla.components.concept.fetch.Request
import org.json.JSONArray
import org.mozilla.focus.search.SearchEngineManager
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.util.safeApiCall
import org.mozilla.rocket.util.sendHttpRequest

class KeywordSuggestionRepository(appContext: Context) {

    private val searchEngine = SearchEngineManager.getInstance().getDefaultSearchEngine(appContext)

    suspend fun fetchSuggestions(keyword: String): Result<List<String>> = withContext(Dispatchers.IO) {
        return@withContext safeApiCall(
            call = {
                sendHttpRequest(request = Request(url = getSuggestionApiEndpoint(keyword), method = Request.Method.GET),
                    onSuccess = {
                        Result.Success(parseSuggestionResult(it.body.string()))
                    },
                    onError = {
                        Result.Error(it)
                    }
                )
            },
            errorMessage = "Unable to get keyword suggestion"
        )
    }

    private fun getSuggestionApiEndpoint(keyword: String): String {
        return searchEngine.buildSearchSuggestionUrl(keyword)
    }

    private fun parseSuggestionResult(response: String): List<String> {
        val suggestions = arrayListOf<String>()
        if (!TextUtils.isEmpty(response)) {
            val jsonArray = JSONArray(response)
            val suggestionItems = jsonArray.getJSONArray(1)
            val size = suggestionItems.length()

            for (i in 0 until size.coerceAtMost(MAX_SUGGESTION_COUNT)) {
                suggestions.add(suggestionItems.getString(i))
            }
        }
        return suggestions
    }

    companion object {
        private const val MAX_SUGGESTION_COUNT = 5
    }
}