package org.mozilla.rocket.nightmode.themed

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.View

open class ThemedRecyclerView : RecyclerView, NightTheme {

    override var themeState = ThemedWidgetUtils.ThemeState.DEFAULT.value

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    /**
     * Utilize adding a custom view state ThemedWidgetUtils.STATE_NIGHT_MODE to drawable states list and turn this state on when night mode is enabled. */
    public override fun onCreateDrawableState(extraSpace: Int): IntArray {
        return if (isNightTheme()) {
            val drawableState = super.onCreateDrawableState(extraSpace + getThemeDrawableState().size)
            View.mergeDrawableStates(drawableState, getThemeDrawableState())
            drawableState
        } else {
            super.onCreateDrawableState(extraSpace)
        }
    }

    override fun notifyRefreshDrawableState() {
        refreshDrawableState()
    }
}