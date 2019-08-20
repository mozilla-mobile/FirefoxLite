package org.mozilla.rocket.home.contenthub.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.MarginLayoutParamsCompat
import org.mozilla.focus.R
import org.mozilla.rocket.extension.dpToPx

class ContentHub : LinearLayout {

    private var clickListener: (() -> Unit)? = null

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
            addView(
                ImageView(context).apply {
                    setImageResource(item.iconResId)
                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                    setBackgroundResource(R.drawable.bg_content_hub_item)
                    elevation = dpToPx(ITEM_ELEVATION_IN_DP).toFloat()
                },
                LayoutParams(dpToPx(ITEM_SIZE_IN_DP), dpToPx(ITEM_SIZE_IN_DP)).apply {
                    if (!isLast) {
                        MarginLayoutParamsCompat.setMarginEnd(this, dpToPx(ITEM_MARGIN_IN_DP))
                    }
                }
            )
        }
    }

    fun setOnItemClickListener(listener: () -> Unit) {
        clickListener = listener
    }

    data class Item(val iconResId: Int)

    companion object {
        private const val PADDING_IN_DP = 12f
        private const val ITEM_SIZE_IN_DP = 44f
        private const val ITEM_MARGIN_IN_DP = 24f
        private const val ITEM_ELEVATION_IN_DP = 4f
    }
}