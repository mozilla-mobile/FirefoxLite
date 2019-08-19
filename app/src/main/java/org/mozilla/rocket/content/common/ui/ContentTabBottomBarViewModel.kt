package org.mozilla.rocket.content.common.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.mozilla.rocket.chrome.BottomBarItemAdapter
import org.mozilla.rocket.chrome.BottomBarItemAdapter.ItemData

class ContentTabBottomBarViewModel : ViewModel() {
    val items = MutableLiveData<List<ItemData>>()

    init {
        refresh()
    }

    fun refresh() {
        val configuredItems = DEFAULT_CONTENT_TAB_BOTTOM_BAR_ITEMS
        items.value.let { currentValue ->
            if (configuredItems != currentValue) {
                items.value = configuredItems
            }
        }
    }

    companion object {
        @JvmStatic
        val DEFAULT_CONTENT_TAB_BOTTOM_BAR_ITEMS = listOf(
            ItemData(BottomBarItemAdapter.TYPE_BACK),
            ItemData(BottomBarItemAdapter.TYPE_REFRESH),
            ItemData(BottomBarItemAdapter.TYPE_SHARE),
            ItemData(BottomBarItemAdapter.TYPE_OPEN_IN_NEW_TAB)
        )
    }

    class Factory : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ContentTabBottomBarViewModel::class.java)) {
                return ContentTabBottomBarViewModel() as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
        }
    }
}
