package org.mozilla.rocket.content

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import org.mozilla.rocket.bhaskar.ItemPojo
import org.mozilla.rocket.bhaskar.Repository

typealias ContentRepository = Repository

class ContentViewModel : ViewModel(), Repository.OnDataChangedListener {
    var repository: ContentRepository? = null
    val items = MutableLiveData<List<ItemPojo>>()

    override fun onDataChanged(itemPojoList: MutableList<ItemPojo>?) {
        items.postValue(itemPojoList)
    }

    fun loadMore() {
        repository?.loadMore()
        // now wait for  OnDataChangedListener.onDataChanged to return the result
    }
}