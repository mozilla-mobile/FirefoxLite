package org.mozilla.rocket.nightmode.themed

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout

open class ThemedRelativeLayout : RelativeLayout {
    private var isNight: Boolean = false

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    public override fun onCreateDrawableState(extraSpace: Int): IntArray {
        return if (isNight) {
            val drawableState = super.onCreateDrawableState(extraSpace + ThemedWidgetUtils.STATE_NIGHT_MODE.size)
            View.mergeDrawableStates(drawableState, ThemedWidgetUtils.STATE_NIGHT_MODE)
            drawableState
        } else {
            super.onCreateDrawableState(extraSpace)
        }
    }

    open fun setNightMode(isNight: Boolean) {
        if (this.isNight != isNight) {
            this.isNight = isNight
            refreshDrawableState()
            invalidate()
        }
    }
}
