package org.mozilla.rocket.content.view

import android.content.Context
import android.util.AttributeSet
import android.util.SparseIntArray
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import org.mozilla.focus.R
import org.mozilla.focus.widget.EqualDistributeGrid
import org.mozilla.rocket.nightmode.themed.ThemedImageButton

class BottomBar : FrameLayout, CoordinatorLayout.AttachedBehavior {
    private lateinit var grid: EqualDistributeGrid
    private var onItemClickListener: OnItemClickListener? = null
    private var onItemLongClickListener: OnItemLongClickListener? = null
    private val itemVisibilities = SparseIntArray()
    private val bottomBarBehavior by lazy { BottomBarBehavior() }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
        initPadding()
        addItemContainer()
    }

    private fun initPadding() {
        val horizontalPadding = resources.getDimensionPixelSize(R.dimen.browser_fixed_menu_horizontal_padding)
        setPadding(horizontalPadding, paddingTop, horizontalPadding, paddingBottom)
    }

    private fun addItemContainer() {
        EqualDistributeGrid(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
        }.let {
            grid = it
            addView(it)
        }
    }

    fun setItems(items: List<BottomBarItem>) {
        grid.removeAllViews()
        items.forEachIndexed { index, item ->
            item.createView(context, grid).apply {
                setOnClickListener { onItemClickListener?.onItemClick(item.type, index) }
                setOnLongClickListener { onItemLongClickListener?.onItemLongClick(item.type, index) ?: false }
            }.let { view ->
                view.visibility = itemVisibilities.get(index, View.VISIBLE)
                item.view = view
                grid.addView(view)
            }
        }
    }

    fun setItemVisibility(position: Int, visibility: Int) {
        grid.getChildAt(position)?.visibility = visibility
        itemVisibilities.append(position, visibility)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        onItemClickListener = listener
    }

    fun setOnItemLongClickListener(listener: OnItemLongClickListener) {
        onItemLongClickListener = listener
    }

    interface OnItemClickListener {
        fun onItemClick(type: Int, position: Int)
    }

    interface OnItemLongClickListener {
        fun onItemLongClick(type: Int, position: Int): Boolean
    }

    abstract class BottomBarItem(val type: Int, val viewId: Int) {
        var view: View? = null

        fun createView(context: Context, parent: ViewGroup): View {
            return onCreateView(context, parent).apply {
                id = viewId
            }
        }

        abstract fun onCreateView(context: Context, parent: ViewGroup): View

        open class ImageItem(
            type: Int,
            id: Int,
            private val drawableResId: Int,
            private val tintResId: Int
        ) : BottomBarItem(type, id) {
            override fun onCreateView(context: Context, parent: ViewGroup): View {
                val contextThemeWrapper = ContextThemeWrapper(context, R.style.MainMenuButton)
                return ThemedImageButton(contextThemeWrapper, null, 0).apply {
                    layoutParams = ViewGroup.LayoutParams(contextThemeWrapper, null)
                    scaleType = ImageView.ScaleType.CENTER
                    setImageResource(drawableResId)
                    imageTintList = ContextCompat.getColorStateList(contextThemeWrapper, tintResId)
                }
            }
        }
    }

    override fun getBehavior(): CoordinatorLayout.Behavior<*> = bottomBarBehavior

    class BottomBarBehavior : HideBottomViewOnScrollBehavior<BottomBar> {

        constructor() : super()

        constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

        private var currentState = STATE_SCROLLED_DOWN

        override fun onNestedScroll(coordinatorLayout: CoordinatorLayout, child: BottomBar, target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int) {
            if (currentState != STATE_SCROLLED_DOWN && dyUnconsumed < 0) {
                slideUp(child)
            } else if (currentState != STATE_SCROLLED_UP && dyUnconsumed > 0) {
                slideDown(child)
            }
        }

        internal fun setState(child: BottomBar, slideUp: Boolean) {
            if (slideUp) {
                slideUp(child)
            } else {
                slideDown(child)
            }
        }

        override fun slideUp(child: BottomBar) {
            super.slideUp(child)
            currentState = STATE_SCROLLED_DOWN
        }

        override fun slideDown(child: BottomBar) {
            super.slideDown(child)
            currentState = STATE_SCROLLED_UP
        }

        companion object {
            private const val STATE_SCROLLED_DOWN = 1
            private const val STATE_SCROLLED_UP = 2
        }
    }
}