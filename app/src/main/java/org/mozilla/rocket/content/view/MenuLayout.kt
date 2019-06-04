package org.mozilla.rocket.content.view

import android.content.Context
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import org.mozilla.focus.R
import org.mozilla.focus.widget.EqualDistributeGrid
import org.mozilla.rocket.extension.dpToPx

class MenuLayout : FrameLayout {
    private lateinit var grid: EqualDistributeGrid
    private var onItemClickListener: OnItemClickListener? = null
    private var onItemLongClickListener: OnItemLongClickListener? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    init {
        addItemContainer()
    }

    private fun addItemContainer() {
        EqualDistributeGrid(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            rowCapacity = ROW_CAPACITY
        }.let {
            grid = it
            addView(it)
        }
    }

    fun setItems(items: List<MenuItem>) {
        grid.removeAllViews()
        items.forEachIndexed { index, item ->
            item.createView(context).apply {
                setOnClickListener { onItemClickListener?.onItemClick(item.type, index) }
                setOnLongClickListener { onItemLongClickListener?.onItemLongClick(item.type, index) ?: false }
            }.let { view ->
                item.view = view
                grid.addView(view)
            }
        }
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

    abstract class MenuItem(val type: Int) {
        var view: View? = null

        abstract fun createView(context: Context): View

        open class TextImageItem(
            type: Int,
            private val textResId: Int,
            private val drawableResId: Int,
            private val tintResId: Int
        ) : MenuItem(type) {
            override fun createView(context: Context): View {
                return LinearLayout(context).apply {
                    layoutParams = ViewGroup.LayoutParams(dpToPx(74f), dpToPx(74f))
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_HORIZONTAL
                    setPadding(paddingLeft, dpToPx(12f), paddingRight, paddingBottom)
                    setBackgroundResource(R.drawable.round_rectangle_ripple)

                    addView(
                            View(context).apply {
                                layoutParams = LinearLayout.LayoutParams(dpToPx(24f), dpToPx(24f))
                                setBackgroundResource(drawableResId)
                                backgroundTintList = ContextCompat.getColorStateList(context, tintResId)
                            }
                    )
                    addView(
                            TextView(ContextThemeWrapper(context, R.style.MenuButtonText)).apply {
                                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                                setText(textResId)
                                setTextColor(ContextCompat.getColorStateList(context, tintResId))
                            }
                    )
                }
            }
        }
    }

    companion object {
        const val ROW_CAPACITY = 4
    }
}