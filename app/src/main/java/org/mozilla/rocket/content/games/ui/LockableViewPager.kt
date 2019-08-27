package org.mozilla.rocket.content.games.ui

import android.content.Context
import android.view.MotionEvent
import android.text.method.Touch.onTouchEvent
import android.util.AttributeSet
import androidx.viewpager.widget.ViewPager

class LockableViewPager : ViewPager {
    private var swipeable: Boolean = false

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        this.swipeable = true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (this.swipeable) {
            super.onTouchEvent(event)
        } else false
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        return if (this.swipeable) {
            super.onInterceptTouchEvent(event)
        } else false
    }

    fun setSwipeable(swipeable: Boolean) {
        this.swipeable = swipeable
    }
}