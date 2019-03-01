package org.mozilla.rocket.content

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import org.mozilla.rocket.bhaskar.ItemPojo
import org.mozilla.rocket.bhaskar.Repository

class ContentViewModel : ViewModel(), Repository.OnDataChangedListener {
    var repository: Repository? = null
    val items = MutableLiveData<List<ItemPojo>>()

    companion object {
        internal const val VISIBLE_THRESHOLD = 10
    }

    override fun onDataChanged(itemPojoList: MutableList<ItemPojo>?) {
        items.postValue(itemPojoList)
    }

    fun loadMore() {
        repository?.loadMore()
        // now wait for  OnDataChangedListener.onDataChanged to return the result
    }
}