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
import org.mozilla.rocket.content.view.BrowserBottomBar
import org.mozilla.rocket.content.view.BrowserBottomBar.BottomBarItem
import org.mozilla.rocket.content.view.BrowserBottomBar.BottomBarItem.ImageItem
import org.mozilla.rocket.nightmode.themed.ThemedImageButton

class BottomBarItemAdapter(
        private val browserBottomBar: BrowserBottomBar,
        private val theme: Theme = Theme.LIGHT
) {
    private var items: List<BottomBarItem>? = null

    fun setItems(types: List<ItemData>) {
        convertToItems(types).let {
            items = it
            browserBottomBar.setItems(it)
        }
    }

    private fun convertToItems(types: List<ItemData>): List<BottomBarItem> =
            types.map(this::convertToItem)

    private fun convertToItem(itemData: ItemData): BottomBarItem {
        val tintResId = theme.buttonColorResId
        return when(val type = itemData.type) {
            TYPE_TAB_COUNTER -> TabCounterItem(type, tintResId)
            TYPE_MENU -> MenuItem(type, theme.buttonColorResId)
            TYPE_NEW_TAB -> ImageItem(type, R.drawable.action_add, tintResId)
            TYPE_SEARCH -> ImageItem(type, R.drawable.action_search, tintResId)
            TYPE_CAPTURE -> ImageItem(type, R.drawable.action_capture, tintResId)
            TYPE_PIN_SHORTCUT -> ImageItem(type, R.drawable.action_add_to_home, tintResId)
            else -> error("Unexpected BottomBarItem ItemType: $type")
        }
    }

    fun setNightMode(isNight: Boolean) {
        items?.forEach {
            val view = it.view
            val type = it.type
            when {
                view is ThemedImageButton -> view.setNightMode(isNight)
                type == TYPE_TAB_COUNTER -> (view as TabCounter).setNightMode(isNight)
                type == TYPE_MENU -> view?.findViewById<ThemedImageButton>(R.id.btn_menu)?.setNightMode(isNight)
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

    private fun getItems(type: Int): List<BottomBarItem> =
            items?.filter { it.type == type } ?: emptyList()

    fun findItem(type: Int): BottomBarItem? =
            items?.find { it.type == type }

    private class TabCounterItem(
            type: Int,
            private val tintResId: Int
    ) : BottomBarItem(type) {
        override fun createView(context: Context): View {
            val contextThemeWrapper = ContextThemeWrapper(context, R.style.MainMenuButton)
            return TabCounter(contextThemeWrapper, null, 0).apply {
                layoutParams = ViewGroup.LayoutParams(contextThemeWrapper, null)
                tintDrawables(ContextCompat.getColorStateList(contextThemeWrapper, tintResId))
            }
        }
    }

    private class MenuItem(
            type: Int,
            private val tintResId: Int
    ) : BottomBarItem(type) {
        override fun createView(context: Context): View {
            return LayoutInflater.from(context)
                    .inflate(R.layout.button_more, null)
                    .also { view ->
                        view.findViewById<ThemedImageButton>(R.id.btn_menu).apply {
                            val contextThemeWrapper = ContextThemeWrapper(context, R.style.MainMenuButton)
                            imageTintList = ContextCompat.getColorStateList(contextThemeWrapper, tintResId)
                        }
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

        const val DOWNLOAD_STATE_DEFAULT = 0
        const val DOWNLOAD_STATE_DOWNLOADING = 1
        const val DOWNLOAD_STATE_UNREAD = 2
        const val DOWNLOAD_STATE_WARNING = 3
    }
}