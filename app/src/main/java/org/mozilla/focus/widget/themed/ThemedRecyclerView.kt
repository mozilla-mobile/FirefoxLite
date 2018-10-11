package org.mozilla.focus.widget.themed

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.View

open class ThemedRecyclerView : RecyclerView {
    private var isNight: Boolean = false

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    /**
     * Utilize adding a custom view state ThemedWidgetUtils.STATE_NIGHT_MODE to drawable states list and turn this state on when night mode is enabled. */
    public override fun onCreateDrawableState(extraSpace: Int): IntArray {
        return if (isNight) {
            val drawableState = super.onCreateDrawableState(extraSpace + ThemedWidgetUtils.STATE_NIGHT_MODE.size)
            View.mergeDrawableStates(drawableState, ThemedWidgetUtils.STATE_NIGHT_MODE)
            drawableState
        } else {
            super.onCreateDrawableState(extraSpace)
        }
    }

    fun isNightMode() : Boolean {
        return isNight
    }

    open fun setNightMode(isNight: Boolean) {
        if (this.isNight != isNight) {
            this.isNight = isNight
            refreshDrawableState()
            invalidate()
        }
    }
}