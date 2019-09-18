package org.mozilla.rocket.shopping.search.data

import android.content.Context
import android.text.TextUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.mozilla.focus.search.SearchEngineManager
import org.mozilla.focus.web.WebViewProvider
import org.mozilla.httprequest.HttpRequest
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.util.safeApiCall
import java.net.URL

class KeywordSuggestionRepository(appContext: Context) {

    private val searchEngine = SearchEngineManager.getInstance().getDefaultSearchEngine(appContext)
    private val userAgent = WebViewProvider.getUserAgentString(appContext)

    suspend fun fetchSuggestions(keyword: String): Result<List<String>> = withContext(Dispatchers.IO) {
        return@withContext safeApiCall(
            call = {
                val responseBody = getHttpResult(getSuggestionApiEndpoint(keyword))
                Result.Success(parseSuggestionResult(responseBody))
            },
            errorMessage = "Unable to get keyword suggestion"
        )
    }

    private fun getHttpResult(endpointUrl: String): String {
        return HttpRequest.get(URL(endpointUrl), userAgent)
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