package org.mozilla.rocket.content

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import org.mozilla.lite.partner.NewsItem
import org.mozilla.lite.partner.Repository

class ContentViewModel : ViewModel(), Repository.OnDataChangedListener<NewsItem> {
    var repository: Repository<out NewsItem>? = null
        set(value) {
            if (field != value) {
                items.value = null
            }
            field = value
        }
    val items = MutableLiveData<List<NewsItem>>()

    override fun onDataChanged(newsItemList: List<NewsItem>?) {
        // return the new list, so diff utils will think this is something to diff
        items.value = newsItemList
    }

    fun loadMore() {
        repository?.loadMore()
        // now wait for OnDataChangedListener.onDataChanged to return the result
    }
}