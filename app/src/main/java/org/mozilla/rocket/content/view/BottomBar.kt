package org.mozilla.rocket.content.view

import android.content.Context
import android.graphics.drawable.Drawable
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
import org.mozilla.rocket.extension.dpToPx
import org.mozilla.rocket.nightmode.themed.ThemedImageButton

open class BottomBar : FrameLayout, CoordinatorLayout.AttachedBehavior {
    protected lateinit var dividerView: View
    private lateinit var grid: EqualDistributeGrid
    private var onItemClickListener: OnItemClickListener? = null
    private var onItemLongClickListener: OnItemLongClickListener? = null
    private val itemVisibilities = SparseIntArray()
    private val bottomBarBehavior by lazy { BottomBarBehavior() }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        init()
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.BottomBar, defStyle, 0)
        val dividerDrawable = typedArray.getDrawable(R.styleable.BottomBar_dividerDrawable)
        typedArray.recycle()
        if (dividerDrawable != null) {
            setDividerDrawable(dividerDrawable)
        }
    }

    private fun init() {
        addDividerView()
        addItemContainer()
    }

    private fun addDividerView() {
        dividerView = createDividerView()
        addView(dividerView, LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(1f)))
    }

    protected open fun createDividerView(): View = View(context)

    private fun addItemContainer() {
        EqualDistributeGrid(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_VERTICAL
                val horizontalPadding = resources.getDimensionPixelSize(R.dimen.browser_fixed_menu_horizontal_padding)
                marginStart = horizontalPadding
                marginEnd = horizontalPadding
            }
        }.let {
            grid = it
            addView(it)
        }
    }

    fun setDividerDrawable(dividerDrawable: Drawable) {
        dividerView.apply {
            background = dividerDrawable
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

    fun onScreenRotated() {
        if (childCount > 0) {
            val itemContainer = getChildAt(childCount - 1) as ViewGroup
            itemContainer.layoutParams = (itemContainer.layoutParams as MarginLayoutParams).apply {
                val horizontalPadding = resources.getDimensionPixelSize(R.dimen.browser_fixed_menu_horizontal_padding)
                marginStart = horizontalPadding
                marginEnd = horizontalPadding
            }
        }
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

            fun BottomBar.slideUp() {
                (behavior as BottomBarBehavior).slideUp(this)
            }

            fun BottomBar.slideDown() {
                (behavior as BottomBarBehavior).slideDown(this)
            }
        }
    }
}