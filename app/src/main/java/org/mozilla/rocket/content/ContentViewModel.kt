package org.mozilla.rocket.content

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import org.mozilla.rocket.bhaskar.ItemPojo
import org.mozilla.rocket.bhaskar.Repository

class ContentViewModel : ViewModel(), Repository.OnDataChangedListener {
    var repository: Repository? = null
    val items = MutableLiveData<List<ItemPojo>>()

    // the library use LiveData as callback to onDataChanged. So here will always on main thread
    override fun onDataChanged(itemPojoList: MutableList<ItemPojo>?) {
        val newList = ArrayList<ItemPojo>()
        // exclude existing items from itemPojoList
        items.value?.let {
            itemPojoList?.removeAll(it)
            // there are new items, add the old item to new list first
            newList.addAll(it)
        }
        // add the new items in the new list
        itemPojoList?.let {
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