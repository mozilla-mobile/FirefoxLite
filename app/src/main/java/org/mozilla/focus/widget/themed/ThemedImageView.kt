package org.mozilla.focus.widget.themed

import android.content.Context
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet
import android.view.View

class ThemedImageView(context: Context, attrs: AttributeSet) : AppCompatImageView(context, attrs) {

    private var isNight: Boolean = false

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
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
