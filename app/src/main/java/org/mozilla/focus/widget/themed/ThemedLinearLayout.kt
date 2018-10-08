package org.mozilla.focus.widget.themed

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout

import org.mozilla.focus.R

class ThemedLinearLayout(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    private var isNight: Boolean = false

    public override fun onCreateDrawableState(extraSpace: Int): IntArray {
        return if (isNight) {
            val drawableState = super.onCreateDrawableState(extraSpace + ThemedWidgetUtils.STATE_NIGHT_MODE.size)
            View.mergeDrawableStates(drawableState, ThemedWidgetUtils.STATE_NIGHT_MODE)
            drawableState
        } else {
             super.onCreateDrawableState(extraSpace)
        }
    }

    fun setNightMode(isNight: Boolean) {
        if (this.isNight != isNight) {
            this.isNight = isNight
            refreshDrawableState()
            invalidate()
        }
    }

}
