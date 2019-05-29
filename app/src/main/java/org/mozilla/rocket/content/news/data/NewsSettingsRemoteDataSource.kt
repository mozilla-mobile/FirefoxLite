package org.mozilla.rocket.content.news.data

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import org.json.JSONArray
import org.mozilla.httprequest.HttpRequest
import org.mozilla.threadutils.ThreadUtils
import java.net.URL

class NewsSettingsRemoteDataSource : NewsSettingsDataSource {
    private val languagesLiveData: MutableLiveData<List<NewsLanguage>> = MutableLiveData()
    private val categoriesLiveData: MutableLiveData<List<String>> = MutableLiveData()

    override fun getSupportLanguages(): LiveData<List<NewsLanguage>> {
        ThreadUtils.postToBackgroundThread {
            var responseBody = getHttpResult(getLanguageApiEndpoint())
            val newsLanguageList = NewsLanguage.fromJson(responseBody)
            languagesLiveData.postValue(newsLanguageList)
        }

        return languagesLiveData
    }

    override fun setSupportLanguages(languages: List<NewsLanguage>) {
        throw UnsupportedOperationException("Can't set news languages setting to server")
    }

    override fun getUserPreferenceLanguage(): LiveData<NewsLanguage> {
        throw UnsupportedOperationException("Can't get user preference news languages setting from server")
    }

    override fun setUserPreferenceLanguage(language: NewsLanguage) {
        throw UnsupportedOperationException("Can't set user preference news languages setting to server")
    }

    override fun getSupportCategories(language: String): LiveData<List<String>> {
        ThreadUtils.postToBackgroundThread {
            var responseBody = getHttpResult(getCategoryApiEndpoint(language))
            val result = ArrayList<String>()
            val items = JSONArray(responseBody)
            for (i in 0 until items.length()) {
                val categoryId = items.optString(i)
                result.add(categoryId)
            }
            categoriesLiveData.postValue(result)
        }

        return categoriesLiveData
    }

    override fun getUserPreferenceCategories(language: String): LiveData<List<String>> {
        throw UnsupportedOperationException("Can't get user preference news category setting from server")
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