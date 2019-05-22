package org.mozilla.rocket.chrome

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import org.mozilla.focus.utils.AppConfigWrapper
import org.mozilla.rocket.chrome.BottomBarItemAdapter.ItemData
import java.util.Arrays

class PrivateBottomBarViewModel : ViewModel() {
    val items = MutableLiveData<List<ItemData>>()

    init {
        refresh()
    }

    fun refresh() {
        val configuredItems = getConfiguredItems() ?: DEFAULT_PRIVATE_BOTTOM_BAR_ITEMS
        items.value.let { currentValue ->
            if (configuredItems != currentValue) {
                items.value = configuredItems
            }
        }
    }

    private fun getConfiguredItems(): List<ItemData>? = AppConfigWrapper.getBottomBarItems()

    companion object {
        @JvmStatic
        val DEFAULT_PRIVATE_BOTTOM_BAR_ITEMS: List<ItemData> = Arrays.asList(
                ItemData(BottomBarItemAdapter.TYPE_PRIVATE_HOME),
                ItemData(BottomBarItemAdapter.TYPE_NEW_TAB),
                ItemData(BottomBarItemAdapter.TYPE_SEARCH),
                ItemData(BottomBarItemAdapter.TYPE_CAPTURE),
                ItemData(BottomBarItemAdapter.TYPE_MENU)
        )
    }
}
