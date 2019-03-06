package org.mozilla.rocket.content

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import org.mozilla.rocket.bhaskar.ItemPojo
import org.mozilla.rocket.bhaskar.Repository

class ContentViewModel : ViewModel(), Repository.OnDataChangedListener {
    var repository: Repository? = null
    val items = MutableLiveData<MutableList<ItemPojo>>()

    init {
        items.value = ArrayList()
    }

    override fun onDataChanged(itemPojoList: MutableList<ItemPojo>?) {
        val newList = ArrayList<ItemPojo>()
        // exclude existing items from new data
        items.value?.let {
            itemPojoList?.removeAll(it)
            // add the old item to new list
            newList.addAll(it)
        }
        // add the new items in new list
        itemPojoList?.let {
            newList.addAll(it)
        }

        // use a new list , so diff utils will knows
        items.postValue(newList)
    }

    fun loadMore() {
        repository?.loadMore()
        // now wait for  OnDataChangedListener.onDataChanged to return the result
    }
}