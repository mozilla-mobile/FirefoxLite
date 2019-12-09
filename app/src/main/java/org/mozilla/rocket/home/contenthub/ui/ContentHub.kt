package org.mozilla.rocket.home.contenthub.ui

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.MarginLayoutParamsCompat
import org.mozilla.focus.R
import org.mozilla.rocket.extension.dpToPx

class ContentHub : LinearLayout {

    private var clickListener: ((Item) -> Unit)? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        orientation = HORIZONTAL
        clipToPadding = false
        dpToPx(PADDING_IN_DP).let { setPadding(it, it, it, it) }
    }

    fun setItems(items: List<Item>) {
        removeAllViews()
        items.forEachIndexed { i, item ->
            val isLast = i == items.size - 1
            val isUnread = item.isUnread
            val itemSize = dpToPx(ITEM_SIZE_IN_DP)
            addView(
                FrameLayout(context).apply {
                    addView(
                        ImageView(context).apply {
                            setImageResource(item.iconResId)
                            scaleType = ImageView.ScaleType.CENTER_INSIDE
                            setBackgroundResource(R.drawable.bg_content_hub_item)
                            elevation = dpToPx(ITEM_ELEVATION_IN_DP).toFloat()
                            setOnClickListener { clickListener?.invoke(item) }
                        },
                        FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                    )
                    if (isUnread) {
                        val unreadDotSize = dpToPx(UNREAD_DOT_SIZE_IN_DP)
                        addView(
                            View(context).apply {
                                setBackgroundResource(R.drawable.content_hub_red_dot)
                                elevation = dpToPx(ITEM_ELEVATION_IN_DP).toFloat()
                            },
                            FrameLayout.LayoutParams(unreadDotSize, unreadDotSize).apply {
                                gravity = Gravity.TOP or Gravity.END
                            }
                        )
                    }
                },
                LayoutParams(itemSize, itemSize).apply {
                    if (!isLast) {
                        MarginLayoutParamsCompat.setMarginEnd(this, dpToPx(ITEM_MARGIN_IN_DP))
                    }
                }
            )
        }
    }

    fun setOnItemClickListener(listener: (Item) -> Unit) {
        clickListener = listener
    }

    sealed class Item(open val iconResId: Int, open var isUnread: Boolean) {
        data class Travel(override val iconResId: Int, override var isUnread: Boolean) : Item(iconResId, isUnread)
        data class Shopping(override val iconResId: Int, override var isUnread: Boolean) : Item(iconResId, isUnread)
        data class News(override val iconResId: Int, override var isUnread: Boolean) : Item(iconResId, isUnread)
        data class Games(override val iconResId: Int, override var isUnread: Boolean) : Item(iconResId, isUnread)
    }

    companion object {
        private const val PADDING_IN_DP = 12f
        private const val ITEM_SIZE_IN_DP = 44f
        private const val ITEM_MARGIN_IN_DP = 24f
        private const val ITEM_ELEVATION_IN_DP = 4f
        private const val UNREAD_DOT_SIZE_IN_DP = 10f
    }
}