package org.mozilla.rocket.content.news

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import org.mozilla.lite.partner.NewsItem
import org.mozilla.lite.partner.Repository
import org.mozilla.rocket.content.news.data.NewsCategory
import org.mozilla.rocket.content.news.data.NewsLanguage
import org.mozilla.rocket.content.news.data.NewsSettingsRepository

class NewsViewModel : ViewModel(), Repository.OnDataChangedListener<NewsItem> {
    var repository: Repository<out NewsItem>? = null
        set(value) {
            if (field != value) {
                items.value = null
            }
            field = value
        }
    val items = MutableLiveData<List<NewsItem>>()

    lateinit var newsSettingsRepository: NewsSettingsRepository

    override fun onDataChanged(newsItemList: List<NewsItem>?) {
        // return the new list, so diff utils will think this is something to diff
        items.value = newsItemList
    }

    fun loadMore() {
        repository?.loadMore()
        // now wait for OnDataChangedListener.onDataChanged to return the result
    }

    fun getSupportLanguages(): LiveData<List<NewsLanguage>> {
        return newsSettingsRepository.getLanguages()
    }

    fun getUserPreferenceLanguage(): LiveData<NewsLanguage> {
        return newsSettingsRepository.getUserPreferenceLanguage()
    }

    fun setUserPreferenceLanguage(language: NewsLanguage) {
        newsSettingsRepository.setUserPreferenceLanguage(language)
    }

    fun getCategoriesByLanguage(language: String): LiveData<List<NewsCategory>> {
        return newsSettingsRepository.getCategoriesByLanguage(language)
    }
}