package org.mozilla.rocket.content

import android.content.Context
import android.support.v4.content.ContextCompat
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.airbnb.lottie.LottieAnimationView
import org.mozilla.focus.R
import org.mozilla.focus.tabs.TabCounter
import org.mozilla.rocket.content.view.BottomBar
import org.mozilla.rocket.content.view.BottomBar.BottomBarItem
import org.mozilla.rocket.content.view.BottomBar.BottomBarItem.ImageItem
import org.mozilla.rocket.nightmode.themed.ThemedImageButton

class BottomBarItemAdapter(
        private val bottomBar: BottomBar,
        private val theme: Theme = Theme.LIGHT
) {
    private var items: List<BottomBarItem>? = null

    fun setItems(types: List<ItemData>) {
        convertToItems(types).let {
            items = it
            bottomBar.setItems(it)
        }
    }

    private fun convertToItems(types: List<ItemData>): List<BottomBarItem> =
            types.map(this::convertToItem)

    private fun convertToItem(itemData: ItemData): BottomBarItem {
        return when(val type = itemData.type) {
            TYPE_TAB_COUNTER -> TabCounterItem(type, theme)
            TYPE_MENU -> MenuItem(type, theme)
            TYPE_NEW_TAB -> ImageItem(type, R.drawable.action_add, theme.buttonColorResId)
            TYPE_SEARCH -> ImageItem(type, R.drawable.action_search, theme.buttonColorResId)
            TYPE_CAPTURE -> ImageItem(type, R.drawable.action_capture, theme.buttonColorResId)
            TYPE_PIN_SHORTCUT -> ImageItem(type, R.drawable.action_add_to_home, theme.buttonColorResId)
            TYPE_BOOKMARK -> BookmarkItem(type, theme)
            TYPE_REFRESH -> RefreshItem(type, theme)
            TYPE_SHARE -> ImageItem(type, R.drawable.action_share, theme.buttonColorResId)
            TYPE_NEXT -> ImageItem(type, R.drawable.action_next, theme.buttonColorResId)
            else -> error("Unexpected BottomBarItem ItemType: $type")
        }
    }

    private fun getItems(type: Int): List<BottomBarItem> =
            items?.filter { it.type == type } ?: emptyList()

    fun findItem(type: Int): BottomBarItem? =
            items?.find { it.type == type }

    fun setNightMode(isNight: Boolean) {
        items?.forEach {
            val view = it.view
            val type = it.type
            when {
                view is ThemedImageButton -> view.setNightMode(isNight)
                type == TYPE_TAB_COUNTER -> (view as TabCounter).setNightMode(isNight)
                type == TYPE_MENU -> view?.findViewById<ThemedImageButton>(R.id.btn_menu)?.setNightMode(isNight)
                type == TYPE_REFRESH -> {
                    view?.findViewById<ThemedImageButton>(R.id.action_refresh)?.setNightMode(isNight)
                    view?.findViewById<ThemedImageButton>(R.id.action_stop)?.setNightMode(isNight)
                }
            }
        }
    }

    @JvmOverloads
    fun setTabCount(count: Int, animationEnabled: Boolean = false) {
        getItems(TYPE_TAB_COUNTER)
                .map { (it as TabCounterItem).view as TabCounter }
                .forEach {
                    if (animationEnabled) {
                        it.setCount(count)
                    } else {
                        it.setCountWithAnimation(count)
                    }
                }
    }

    fun setDownloadState(state: Int) {
        getItems(TYPE_MENU).forEach {
            val view = requireNotNull(it.view)
            val stateIcon = view.findViewById<ImageView>(R.id.download_unread_indicator)
            val downloadingAnimationView = view.findViewById<LottieAnimationView>(R.id.downloading_indicator)
            when (state) {
                DOWNLOAD_STATE_DEFAULT -> {
                    stateIcon.visibility = View.GONE
                    downloadingAnimationView.visibility = View.GONE
                }
                DOWNLOAD_STATE_DOWNLOADING -> {
                    stateIcon.visibility = View.GONE
                    downloadingAnimationView.apply {
                        visibility = View.VISIBLE
                        if (!downloadingAnimationView.isAnimating) {
                            playAnimation()
                        }
                    }
                }
                DOWNLOAD_STATE_UNREAD -> {
                    stateIcon.apply {
                        visibility = View.VISIBLE
                        setImageResource(R.drawable.notify_download)
                    }
                    downloadingAnimationView.visibility = View.GONE
                }
                DOWNLOAD_STATE_WARNING -> {
                    stateIcon.apply {
                        visibility = View.VISIBLE
                        setImageResource(R.drawable.notify_notice)
                    }
                    downloadingAnimationView.visibility = View.GONE
                }
            }
        }
    }

    fun setBookmark(isBookmark: Boolean) {
        getItems(TYPE_BOOKMARK)
                .forEach {
                    it.view?.isActivated = isBookmark
                }
    }

    fun setRefreshing(isRefreshing: Boolean) {
        getItems(TYPE_REFRESH)
                .forEach {
                    val refreshIcon = it.view?.findViewById<ThemedImageButton>(R.id.action_refresh)
                    val stopIcon = it.view?.findViewById<ThemedImageButton>(R.id.action_stop)
                    if (isRefreshing) {
                        refreshIcon?.visibility = View.INVISIBLE
                        stopIcon?.visibility = View.VISIBLE
                    } else {
                        refreshIcon?.visibility = View.VISIBLE
                        stopIcon?.visibility = View.INVISIBLE
                    }
                }
    }

    fun setCanGoForward(canGoForward: Boolean) {
        getItems(TYPE_NEXT)
                .forEach {
                    it.view?.isEnabled = canGoForward
                }
    }

    private class TabCounterItem(
            type: Int,
            private val theme: Theme
    ) : BottomBarItem(type) {
        override fun createView(context: Context): View {
            val contextThemeWrapper = ContextThemeWrapper(context, R.style.MainMenuButton)
            return TabCounter(contextThemeWrapper, null, 0).apply {
                layoutParams = ViewGroup.LayoutParams(contextThemeWrapper, null)
                tintDrawables(ContextCompat.getColorStateList(contextThemeWrapper, theme.buttonColorResId))
            }
        }
    }

    private class MenuItem(
            type: Int,
            private val theme: Theme
    ) : BottomBarItem(type) {
        override fun createView(context: Context): View {
            return LayoutInflater.from(context)
                    .inflate(R.layout.button_more, null)
                    .apply {
                        findViewById<ThemedImageButton>(R.id.btn_menu).setTint(context, theme.buttonColorResId)
                    }
        }
    }

    private class BookmarkItem(type: Int, theme: Theme) : ImageItem(
            type,
            R.drawable.ic_add_bookmark,
            if (theme == Theme.LIGHT) R.color.ic_add_bookmark_tint_light else R.color.ic_add_bookmark_tint_dark
    )

    private class RefreshItem(
            type: Int,
            private val theme: Theme
    ) : BottomBarItem(type) {
        override fun createView(context: Context): View {
            return LayoutInflater.from(context)
                    .inflate(R.layout.button_refresh, null)
                    .also { view ->
                        view.findViewById<ThemedImageButton>(R.id.action_refresh).setTint(context, theme.buttonColorResId)
                        view.findViewById<ThemedImageButton>(R.id.action_stop).setTint(context, theme.buttonColorResId)
                    }
        }
    }

    sealed class Theme(val buttonColorResId: Int) {
        object LIGHT : Theme(buttonColorResId = R.color.browser_menu_button)
        object DARK : Theme(buttonColorResId = R.color.home_bottom_button)
    }

    data class ItemData(val type: Int)

    companion object {
        const val TYPE_TAB_COUNTER = 0
        const val TYPE_MENU = 1
        const val TYPE_NEW_TAB = 2
        const val TYPE_SEARCH = 3
        const val TYPE_CAPTURE = 4
        const val TYPE_PIN_SHORTCUT = 5
        const val TYPE_BOOKMARK = 6
        const val TYPE_REFRESH = 7
        const val TYPE_SHARE = 8
        const val TYPE_NEXT = 9

        const val DOWNLOAD_STATE_DEFAULT = 0
        const val DOWNLOAD_STATE_DOWNLOADING = 1
        const val DOWNLOAD_STATE_UNREAD = 2
        const val DOWNLOAD_STATE_WARNING = 3
    }
}

private fun ImageView.setTint(context: Context, colorResId: Int) {
    val contextThemeWrapper = ContextThemeWrapper(context, 0)
    imageTintList = ContextCompat.getColorStateList(contextThemeWrapper, colorResId)
}