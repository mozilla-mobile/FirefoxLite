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

    override fun onDataChanged(NewsItemList: MutableList<NewsItem>?) {
        val newList = ArrayList<NewsItem>()
        // exclude existing items from NewsItemList
        items.value?.let {
            NewsItemList?.removeAll(it)
            // there are new items, add the old item to new list first
            newList.addAll(it)
        }
        // add the new items in the new list
        NewsItemList?.let {
            newList.addAll(it)
        }

        // return the new list , so diff utils will think this is something to diff
        items.value = newList
    }

    fun loadMore() {
        repository?.loadMore()
        // now wait for  OnDataChangedListener.onDataChanged to return the result
    }
}