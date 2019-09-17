package org.mozilla.rocket.content.news.ui

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import org.mozilla.lite.partner.NewsItem
import org.mozilla.lite.partner.Repository
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.news.domain.LoadNewsParameter
import org.mozilla.rocket.content.news.domain.LoadNewsUseCase

class NewsViewModel : ViewModel() {

    private val newsMap = HashMap<String, MediatorLiveData<List<NewsItem>>>()

    private val useCaseMap = HashMap<String, LoadNewsUseCase>()

    fun clear() {
        newsMap.clear()
        useCaseMap.clear()
    }

    fun getNews(category: String, lang: String, repo: Repository<out NewsItem>): MediatorLiveData<List<NewsItem>> {
        val items = newsMap[category] ?: MediatorLiveData()
        if (newsMap[category] == null) {
            newsMap[category] = items
            HashMap<String, String>().apply {
                this["category"] = category
                this["lang"] = lang
            }
            val loadNewsCase = LoadNewsUseCase(repo)
            useCaseMap[category] = loadNewsCase
            items.addSource(loadNewsCase.observe()) { result ->
                (result as? Result.Success)?.data?.let {
                    items.value = it.items
                }
            }
        }
        return items
    }

    fun loadMore(category: String) {
        useCaseMap[category]?.execute(LoadNewsParameter(category))
    }
}