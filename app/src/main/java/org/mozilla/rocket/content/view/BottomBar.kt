package org.mozilla.rocket.content.view

import android.content.Context
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.util.SparseIntArray
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import org.mozilla.focus.R
import org.mozilla.focus.widget.EqualDistributeGrid
import org.mozilla.rocket.nightmode.themed.ThemedImageButton

class BottomBar : FrameLayout {
    private lateinit var grid: EqualDistributeGrid
    private var onItemClickListener: OnItemClickListener? = null
    private var onItemLongClickListener: OnItemLongClickListener? = null
    private val itemVisibilities = SparseIntArray()

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
            item.createView(context).apply {
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

    abstract class BottomBarItem(val type: Int) {
        var view: View? = null

        abstract fun createView(context: Context): View

        open class ImageItem(
            type: Int,
            private val drawableResId: Int,
            private val tintResId: Int
        ) : BottomBarItem(type) {
            override fun createView(context: Context): View {
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
}