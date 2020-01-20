package org.mozilla.rocket.content.news.data.newspoint

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mozilla.components.concept.fetch.Request
import org.json.JSONArray
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.Result.Success
import org.mozilla.rocket.content.news.data.NewsCategory
import org.mozilla.rocket.content.news.data.NewsLanguage
import org.mozilla.rocket.content.news.data.NewsProvider
import org.mozilla.rocket.content.news.data.NewsSettingsDataSource
import org.mozilla.rocket.content.news.data.NewsSourceInfo
import org.mozilla.rocket.content.news.data.dailyhunt.DailyHuntProvider
import org.mozilla.rocket.util.safeApiCall
import org.mozilla.rocket.util.sendHttpRequest

class NewsPointSettingsRemoteDataSource(
    private val newsProvider: NewsProvider?,
    private val dailyHuntProvider: DailyHuntProvider?
) : NewsSettingsDataSource {

    override suspend fun getSupportLanguages(): Result<List<NewsLanguage>> = withContext(Dispatchers.IO) {
        return@withContext safeApiCall(
            call = {
                sendHttpRequest(request = Request(url = getLanguageApiEndpoint(), method = Request.Method.GET),
                    onSuccess = {
                        Success(NewsLanguage.fromJson(it.body.string()))
                    },
                    onError = {
                        Result.Error(it)
                    }
                )
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

    override suspend fun getSupportCategories(language: String): Result<List<NewsCategory>> = withContext(Dispatchers.IO) {
        return@withContext safeApiCall(
            call = {
                sendHttpRequest(request = Request(url = getCategoryApiEndpoint(language), method = Request.Method.GET),
                    onSuccess = {
                        Success(parseCategoriesResult(it.body.string()))
                    },
                    onError = {
                        Result.Error(it)
                    }
                )
            },
            errorMessage = "Unable to get remote news categories"
        )
    }

    override suspend fun setSupportCategories(language: String, supportCategories: List<NewsCategory>) {
        throw UnsupportedOperationException("Can't set news categories to server")
    }

    override suspend fun getUserPreferenceCategories(language: String): Result<List<NewsCategory>> {
        throw UnsupportedOperationException("Can't get user preference news category setting from server")
    }

    override suspend fun setUserPreferenceCategories(language: String, userPreferenceCategories: List<NewsCategory>) {
        throw UnsupportedOperationException("Can't set user preference news category setting to server")
    }

    override fun getDefaultLanguage(): NewsLanguage {
        throw UnsupportedOperationException("Can't get default language setting from server")
    }

    override fun getDefaultCategory(): NewsCategory {
        throw UnsupportedOperationException("Can't get default category setting from server")
    }

    override fun getAdditionalSourceInfo(): NewsSourceInfo? {
        throw UnsupportedOperationException("Can't get the additional source info from server")
    }

    override fun shouldEnableNewsSettings(): Boolean {
        throw UnsupportedOperationException("Can't get menu setting from server")
    }

    override fun shouldEnablePersonalizedNews() = dailyHuntProvider?.isEnableFromRemote ?: false

    override fun hasUserEnabledPersonalizedNews(): Boolean {
        throw UnsupportedOperationException("Can't get personalized news user setting from server")
    }

    override fun setUserEnabledPersonalizedNews(enable: Boolean) {
        throw UnsupportedOperationException("Can't set personalized news user setting to server")
    }

    override fun shouldShowPersonalizedNewsOnboarding(): Boolean {
        throw UnsupportedOperationException("Can't get onboarding setting from server")
    }

    override fun setPersonalizedNewsOnboardingHasShown() {
        throw UnsupportedOperationException("Can't get onboarding setting from server")
    }

    override fun shouldShowNewsLanguageSettingPage(): Boolean {
        throw UnsupportedOperationException("Can't get onboarding setting from server")
    }

    override fun setNewsLanguageSettingPageState(enable: Boolean) {
        throw UnsupportedOperationException("Can't get onboarding setting from server")
    }

    private fun getLanguageApiEndpoint(): String {
        return newsProvider?.languagesUrl ?: DEFAULT_LANGUAGE_LIST_URL
    }

    private fun getCategoryApiEndpoint(language: String): String {
        val url = newsProvider?.categoriesUrl ?: DEFAULT_CATEGORY_LIST_URL
        return String.format(url, language)
    }

    private fun parseCategoriesResult(jsonString: String): List<NewsCategory> {
        val result = ArrayList<NewsCategory>()
        val items = JSONArray(jsonString)
        for (i in 0 until items.length()) {
            val categoryId = items.optString(i)
            NewsCategory.getCategoryById(categoryId)?.let {
                result.add(it)
            }
        }
        return result
    }

    companion object {
        private const val DEFAULT_LANGUAGE_LIST_URL = "https://envoy.indiatimes.com/NPRSS/language/names"
        private const val DEFAULT_CATEGORY_LIST_URL = "https://envoy.indiatimes.com/NPRSS/pivot/section?lang=%s"
    }
}