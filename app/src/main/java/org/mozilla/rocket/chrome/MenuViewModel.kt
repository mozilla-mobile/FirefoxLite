package org.mozilla.rocket.chrome

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import org.mozilla.focus.utils.AppConfigWrapper
import org.mozilla.rocket.chrome.BottomBarItemAdapter.ItemData
import java.util.Arrays

class MenuViewModel : ViewModel() {
    val bottomItems = MutableLiveData<List<ItemData>>()
    val isBottomBarEnabled = MutableLiveData<Boolean>()

    init {
        isBottomBarEnabled.value = true

        refresh()
    }

    fun refresh(): Boolean {
        var hasNewConfig = false
        val configuredItems = getConfiguredItems() ?: DEFAULT_MENU_BOTTOM_ITEMS
        bottomItems.value.let { currentValue ->
            if (configuredItems != currentValue) {
                bottomItems.value = configuredItems
                hasNewConfig = true
            }
        }

        return hasNewConfig
    }

    private fun getConfiguredItems(): List<ItemData>? = AppConfigWrapper.getMenuBottomBarItems()

    /**
     * TODO: temporary method, directly call the onGainTabFocus() and onLostTabFocus() when
     *  we be able to observe the focus changed event
     */
    fun onTabFocusChanged(hasFocusTab: Boolean) {
        if (hasFocusTab) {
            onGainTabFocus()
        } else {
            onLostTabFocus()
        }
    }

    fun onGainTabFocus() {
        if (isBottomBarEnabled.value != true) {
            isBottomBarEnabled.value = true
        }
    }

    fun onLostTabFocus() {
        if (isBottomBarEnabled.value == true) {
            isBottomBarEnabled.value = false
        }
    }

    companion object {
        @JvmStatic
        val DEFAULT_MENU_BOTTOM_ITEMS: List<ItemData> = Arrays.asList(
                ItemData(BottomBarItemAdapter.TYPE_NEXT),
                ItemData(BottomBarItemAdapter.TYPE_REFRESH),
                ItemData(BottomBarItemAdapter.TYPE_BOOKMARK),
                ItemData(BottomBarItemAdapter.TYPE_PIN_SHORTCUT),
                ItemData(BottomBarItemAdapter.TYPE_SHARE)
        )
    }
}
