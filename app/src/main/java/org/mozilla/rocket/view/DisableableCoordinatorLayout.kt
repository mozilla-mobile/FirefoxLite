package org.mozilla.rocket.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout

open class DisableableCoordinatorLayout : CoordinatorLayout {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var isActive: Boolean = true

    override fun setActivated(activated: Boolean) {
        isActive = activated
    }

    override fun isActivated(): Boolean {
        return isActive
    }

    override fun onStartNestedScroll(child: View, target: View, axes: Int, type: Int): Boolean {
        return isActive && super.onStartNestedScroll(child, target, axes, type)
    }
}