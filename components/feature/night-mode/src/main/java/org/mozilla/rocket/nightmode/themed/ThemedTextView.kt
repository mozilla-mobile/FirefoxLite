package org.mozilla.rocket.nightmode.themed

import android.content.Context
import android.util.AttributeSet
import android.view.View

class ThemedTextView(context: Context, attrs: AttributeSet) : android.support.v7.widget.AppCompatTextView(context, attrs) {
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
