package org.mozilla.rocket.shopping.search.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.mozilla.rocket.chrome.BottomBarItemAdapter
import org.mozilla.rocket.chrome.BottomBarItemAdapter.ItemData

class ShoppingSearchBottomBarViewModel : ViewModel() {
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
            ItemData(BottomBarItemAdapter.TYPE_HOME),
            ItemData(BottomBarItemAdapter.TYPE_REFRESH),
            ItemData(BottomBarItemAdapter.TYPE_SHOPPING_SEARCH),
            ItemData(BottomBarItemAdapter.TYPE_NEXT),
            ItemData(BottomBarItemAdapter.TYPE_SHARE)
        )
    }
}
