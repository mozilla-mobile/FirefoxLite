package org.mozilla.rocket.content.news.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.mozilla.httprequest.HttpRequest
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.Result.Success
import org.mozilla.rocket.util.safeApiCall
import java.net.URL

class NewsSettingsRemoteDataSource : NewsSettingsDataSource {

    override suspend fun getSupportLanguages(): Result<List<NewsLanguage>> = withContext(Dispatchers.Default) {
        return@withContext safeApiCall(
            call = {
                val responseBody = getHttpResult(getLanguageApiEndpoint())
                Success(NewsLanguage.fromJson(responseBody))
            },
            errorMessage = "Unable to get remote news languages"
        )
    }

    override suspend fun setSupportLanguages(languages: List<NewsLanguage>) {
        throw UnsupportedOperationException("Can't set news languages setting to server")
    }

    override suspend fun getUserPreferenceLanguage(): Result<NewsLanguage?> {
        throw UnsupportedOperationException("Can't get user preference news languages setting from server")
    }

    override suspend fun setUserPreferenceLanguage(language: NewsLanguage) {
        throw UnsupportedOperationException("Can't set user preference news languages setting to server")
    }

    override suspend fun getSupportCategories(language: String): Result<List<String>> = withContext(Dispatchers.Default) {
        return@withContext safeApiCall(
            call = {
                val responseBody = getHttpResult(getCategoryApiEndpoint(language))
                val result = ArrayList<String>()
                val items = JSONArray(responseBody)
                for (i in 0 until items.length()) {
                    val categoryId = items.optString(i)
                    result.add(categoryId)
                }
                Success(result)
            },
            errorMessage = "Unable to get remote news categories"
        )
    }

    override suspend fun setSupportCategories(language: String, supportCategories: List<String>) {
        throw UnsupportedOperationException("Can't set news categories to server")
    }

    override suspend fun getUserPreferenceCategories(language: String): Result<List<String>> {
        throw UnsupportedOperationException("Can't get user preference news category setting from server")
    }

    override suspend fun setUserPreferenceCategories(language: String, userPreferenceCategories: List<String>) {
        throw UnsupportedOperationException("Can't set user preference news category setting to server")
    }

    private fun getLanguageApiEndpoint(): String {
        return "https://envoy.indiatimes.com/NPRSS/language/names"
    }

    private fun getCategoryApiEndpoint(language: String): String {
        return "https://envoy.indiatimes.com/NPRSS/pivot/section?lang=$language"
    }

    private fun getHttpResult(endpointUrl: String): String {
        var responseBody = HttpRequest.get(URL(endpointUrl), "")
        responseBody = responseBody.replace("\n", "")
        return responseBody
    }
}