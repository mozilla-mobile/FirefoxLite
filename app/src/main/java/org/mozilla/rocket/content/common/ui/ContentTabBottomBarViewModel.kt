package org.mozilla.rocket.content.common.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
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
            ItemData(BottomBarItemAdapter.TYPE_SHARE)
        )
    }
}
