package org.mozilla.rocket.chrome

import android.view.View
import android.view.ViewGroup
import org.mozilla.focus.R
import org.mozilla.rocket.content.view.MenuLayout
import org.mozilla.rocket.content.view.MenuLayout.MenuItem

class MenuItemAdapter(
    private val menuLayout: MenuLayout,
    private val theme: Theme = Theme.Light
) {
    private var items: List<MenuItem>? = null

    fun setItems(types: List<ItemData>) {
        val hasDuplicate = types.groupBy { it }.size < types.size
        require(!hasDuplicate) { "Cannot set duplicated items to MenuItemAdapter" }

        convertToItems(types).let {
            items = it
            menuLayout.setItems(it)
        }
    }

    private fun convertToItems(types: List<ItemData>): List<MenuItem> =
            types.map(this::convertToItem)

    private fun convertToItem(itemData: ItemData): MenuItem {
        return when (val type = itemData.type) {
            TYPE_BOOKMARKS -> MenuItem.TextImageItem(type, R.id.menu_bookmark, R.string.label_menu_bookmark, R.drawable.ic_bookmarks, theme.tintResId)
            TYPE_DOWNLOADS -> MenuItem.TextImageItem(type, R.id.menu_download, R.string.label_menu_download, R.drawable.menu_download, theme.tintResId)
            TYPE_HISTORY -> MenuItem.TextImageItem(type, R.id.menu_history, R.string.label_menu_history, R.drawable.menu_history, theme.tintResId)
            TYPE_SCREENSHOTS -> MenuItem.TextImageItem(type, R.id.menu_screenshots, R.string.label_menu_my_shots, R.drawable.menu_my_shots_states, null)
            TYPE_TURBO_MODE -> MenuItem.TextImageItem(type, R.id.menu_turbomode, R.string.label_menu_turbo_mode, R.drawable.menu_speedmode, theme.tintResId)
            TYPE_PRIVATE_BROWSING -> MenuItem.TextImageItem(type, R.id.private_browsing_btn, R.string.private_tab, R.drawable.private_browsing_mask_states, null)
            TYPE_NIGHT_MODE -> MenuItem.TextImageItem(type, R.id.menu_night_mode, R.string.label_menu_night_mode, R.drawable.icon_night_mode, theme.tintResId)
            TYPE_BLOCK_IMAGE -> MenuItem.TextImageItem(type, R.id.menu_blockimg, R.string.label_menu_block_image, R.drawable.menu_blockimg, theme.tintResId)
            TYPE_FIND_IN_PAGE -> MenuItem.TextImageItem(type, R.id.menu_find_in_page, R.string.label_menu_find_in_page, R.drawable.ic_menu_find_in_page, theme.tintResId)
            TYPE_CLEAR_CACHE -> MenuItem.TextImageItem(type, R.id.menu_delete, R.string.label_menu_clear_cache, R.drawable.menu_delete, theme.tintResId)
            TYPE_PREFERENCES -> MenuItem.TextImageItem(type, R.id.menu_preferences, R.string.label_menu_settings, R.drawable.menu_settings, theme.tintResId)
            TYPE_EXIT_APP -> MenuItem.TextImageItem(type, R.id.menu_exit, R.string.label_menu_exit, R.drawable.menu_exit, theme.tintResId)
            else -> error("Unexpected BottomBarItem ItemType: $type")
        }
    }

    fun getItem(type: Int): MenuItem? = items?.find { it.type == type }

    fun setEnabled(enabled: Boolean) {
        items?.forEach {
            it.view?.let { view ->
                setEnabled(view, enabled)
            }
        }
    }

    private fun setEnabled(view: View, enabled: Boolean) {
        view.isEnabled = enabled
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                setEnabled(view.getChildAt(i), enabled)
            }
        }
    }

    fun setTurboMode(isTurboMode: Boolean) {
        getItem(TYPE_TURBO_MODE)?.view?.apply {
            isActivated = isTurboMode
        }
    }

    fun setNightMode(isNightMode: Boolean) {
        getItem(TYPE_NIGHT_MODE)?.view?.apply {
            isActivated = isNightMode
        }
    }

    fun setBlockImageEnabled(isEnabled: Boolean) {
        getItem(TYPE_BLOCK_IMAGE)?.view?.apply {
            isActivated = isEnabled
        }
    }

    fun setUnreadScreenshot(unread: Boolean) {
        getItem(TYPE_SCREENSHOTS)?.view?.apply {
            isActivated = unread
        }
    }

    fun setPrivateBrowsingActive(active: Boolean) {
        getItem(TYPE_PRIVATE_BROWSING)?.view?.apply {
            isActivated = active
        }
    }

    fun setFindInPageEnabled(enabled: Boolean) {
        getItem(TYPE_FIND_IN_PAGE)?.view?.let {
            setEnabled(it, enabled)
        }
    }

    sealed class Theme(val tintResId: Int) {
        object Light : Theme(tintResId = R.color.sheet_menu_button)
    }

    data class ItemData(val type: Int)

    companion object {
        const val TYPE_BOOKMARKS = 0
        const val TYPE_DOWNLOADS = 1
        const val TYPE_HISTORY = 2
        const val TYPE_SCREENSHOTS = 3
        const val TYPE_TURBO_MODE = 4
        const val TYPE_PRIVATE_BROWSING = 5
        const val TYPE_NIGHT_MODE = 6
        const val TYPE_BLOCK_IMAGE = 7
        const val TYPE_FIND_IN_PAGE = 8
        const val TYPE_CLEAR_CACHE = 9
        const val TYPE_PREFERENCES = 10
        const val TYPE_EXIT_APP = 11
    }
}