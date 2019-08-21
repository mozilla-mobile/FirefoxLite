package org.mozilla.rocket.home.topsites.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.MarginLayoutParamsCompat
import org.mozilla.focus.R
import org.mozilla.rocket.extension.dpToPx

class PagerIndicator : LinearLayout {

    private var selectedIndex = 0

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        orientation = HORIZONTAL
        minimumHeight = dpToPx(DOT_SIZE_IN_DP)
    }

    fun setSize(size: Int) {
        if (childCount == size) {
            return
        }
        if (selectedIndex >= size) {
            selectedIndex = size - 1
        }

        removeAllViews()
        for (i in 0 until size) {
            val isLast = i == size - 1
            addView(
                View(context).apply {
                    setBackgroundResource(R.drawable.pager_dot)
                    isSelected = i == selectedIndex
                },
                LayoutParams(dpToPx(DOT_SIZE_IN_DP), dpToPx(DOT_SIZE_IN_DP)).apply {
                    if (!isLast) {
                        MarginLayoutParamsCompat.setMarginEnd(this, dpToPx(DOT_MARGIN))
                    }
                }
            )
        }
    }

    fun setSelection(index: Int) {
        if (selectedIndex == index) {
            return
        }

        getChildAt(selectedIndex)?.run {
            isSelected = false
        }
        getChildAt(index)?.run {
            isSelected = true
        }
        selectedIndex = index
    }

    companion object {
        private const val DOT_SIZE_IN_DP = 6f
        private const val DOT_MARGIN = 4f
    }
}