package org.mozilla.rocket.content

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import java.util.*

class BottomBarViewModel : ViewModel() {
    val items = MutableLiveData<List<Int>>()

    init {
        refresh()
    }

    fun refresh() { // TODO: also call this function when creating BrowserFragment
        val configuredItems = getConfiguredItems() ?: DEFAULT_BOTTOM_BAR_ITEMS
        items.value.let { currentValue ->
            if (configuredItems != currentValue) {
                items.value = configuredItems
            }
        }
    }

    private fun getConfiguredItems(): List<Int>? {
        // TODO: fetch configs from firebase
        return null
    }

    companion object {
        private val DEFAULT_BOTTOM_BAR_ITEMS = Arrays.asList(
                BottomBarItemAdapter.TYPE_TAB_COUNTER,
                BottomBarItemAdapter.TYPE_NEW_TAB,
                BottomBarItemAdapter.TYPE_SEARCH,
                BottomBarItemAdapter.TYPE_CAPTURE,
                BottomBarItemAdapter.TYPE_MENU
        )
    }
}
