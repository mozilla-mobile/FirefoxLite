package org.mozilla.rocket.content

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import org.mozilla.focus.utils.AppConfigWrapper
import org.mozilla.rocket.content.BottomBarItemAdapter.ItemData
import java.util.*

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
        private val DEFAULT_BOTTOM_BAR_ITEMS = Arrays.asList(
                ItemData(BottomBarItemAdapter.TYPE_TAB_COUNTER),
                ItemData(BottomBarItemAdapter.TYPE_NEW_TAB),
                ItemData(BottomBarItemAdapter.TYPE_SEARCH),
                ItemData(BottomBarItemAdapter.TYPE_CAPTURE),
                ItemData(BottomBarItemAdapter.TYPE_MENU)
        )
    }
}
