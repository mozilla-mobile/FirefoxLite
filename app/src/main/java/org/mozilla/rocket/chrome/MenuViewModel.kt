package org.mozilla.rocket.chrome

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.mozilla.focus.utils.AppConfigWrapper

class MenuViewModel : ViewModel() {
    val menuItems = MutableLiveData<List<MenuItemAdapter.ItemData>>()
    val bottomItems = MutableLiveData<List<BottomBarItemAdapter.ItemData>>()
    val isBottomBarEnabled = MutableLiveData<Boolean>()

    init {
        isBottomBarEnabled.value = true

        refresh()
    }

    fun refresh() {
        val configuredMenuItems = getConfiguredMenuItems() ?: DEFAULT_MENU_ITEMS
        val configuredBottomBarItems = getConfiguredBottomBarItems() ?: DEFAULT_MENU_BOTTOM_ITEMS
        menuItems.value.let { currentValue ->
            if (configuredMenuItems != currentValue) {
                menuItems.value = configuredMenuItems
            }
        }
        bottomItems.value.let { currentValue ->
            if (configuredBottomBarItems != currentValue) {
                bottomItems.value = configuredBottomBarItems
            }
        }
    }

    private fun getConfiguredMenuItems(): List<MenuItemAdapter.ItemData>? = AppConfigWrapper.getMenuItems()

    private fun getConfiguredBottomBarItems(): List<BottomBarItemAdapter.ItemData>? = AppConfigWrapper.getMenuBottomBarItems()

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
        val DEFAULT_MENU_ITEMS: List<MenuItemAdapter.ItemData> = listOf(
            MenuItemAdapter.ItemData(MenuItemAdapter.TYPE_BOOKMARKS),
            MenuItemAdapter.ItemData(MenuItemAdapter.TYPE_DOWNLOADS),
            MenuItemAdapter.ItemData(MenuItemAdapter.TYPE_HISTORY),
            MenuItemAdapter.ItemData(MenuItemAdapter.TYPE_SCREENSHOTS),
            MenuItemAdapter.ItemData(MenuItemAdapter.TYPE_TURBO_MODE),
            MenuItemAdapter.ItemData(MenuItemAdapter.TYPE_PRIVATE_BROWSING),
            MenuItemAdapter.ItemData(MenuItemAdapter.TYPE_NIGHT_MODE),
            MenuItemAdapter.ItemData(MenuItemAdapter.TYPE_BLOCK_IMAGE),
            MenuItemAdapter.ItemData(MenuItemAdapter.TYPE_FIND_IN_PAGE),
            MenuItemAdapter.ItemData(MenuItemAdapter.TYPE_CLEAR_CACHE),
            MenuItemAdapter.ItemData(MenuItemAdapter.TYPE_PREFERENCES),
            MenuItemAdapter.ItemData(MenuItemAdapter.TYPE_EXIT_APP)
        )
        @JvmStatic
        val DEFAULT_MENU_BOTTOM_ITEMS: List<BottomBarItemAdapter.ItemData> = listOf(
            BottomBarItemAdapter.ItemData(BottomBarItemAdapter.TYPE_NEXT),
            BottomBarItemAdapter.ItemData(BottomBarItemAdapter.TYPE_CAPTURE),
            BottomBarItemAdapter.ItemData(BottomBarItemAdapter.TYPE_BOOKMARK),
            BottomBarItemAdapter.ItemData(BottomBarItemAdapter.TYPE_PIN_SHORTCUT),
            BottomBarItemAdapter.ItemData(BottomBarItemAdapter.TYPE_SHARE)
        )
    }
}
