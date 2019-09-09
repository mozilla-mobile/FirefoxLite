package org.mozilla.rocket.chrome

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.mozilla.focus.utils.AppConfigWrapper
import org.mozilla.rocket.chrome.BottomBarItemAdapter.ItemData

class BottomBarViewModel : ViewModel() {
    val items = MutableLiveData<List<ItemData>>()

    init {
        refresh()
    }

    fun refresh() {
        val configuredItems = getConfiguredItems() ?: DEFAULT_BOTTOM_BAR_ITEMS
        items.value.let { currentValue ->
            if (configuredItems != currentValue) {
                items.value = configuredItems
            }
        }
    }

    private fun getConfiguredItems(): List<ItemData>? = AppConfigWrapper.getBottomBarItems()

    companion object {
        @JvmStatic
        val DEFAULT_BOTTOM_BAR_ITEMS: List<ItemData> = listOf(
            ItemData(BottomBarItemAdapter.TYPE_HOME),
            ItemData(BottomBarItemAdapter.TYPE_REFRESH),
            ItemData(BottomBarItemAdapter.TYPE_SEARCH),
            ItemData(BottomBarItemAdapter.TYPE_TAB_COUNTER),
            ItemData(BottomBarItemAdapter.TYPE_MENU)
        )
    }
}
