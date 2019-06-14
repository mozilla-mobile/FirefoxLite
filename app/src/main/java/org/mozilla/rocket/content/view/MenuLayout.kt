package org.mozilla.rocket.content.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.menu_item_text_image.view.menu_item_image
import kotlinx.android.synthetic.main.menu_item_text_image.view.menu_item_text
import org.mozilla.focus.R
import org.mozilla.focus.widget.EqualDistributeGrid

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
            item.createView(context, grid).apply {
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

    abstract class MenuItem(val type: Int, val viewId: Int) {
        var view: View? = null

        abstract fun createView(context: Context, parent: ViewGroup): View

        open class TextImageItem(
            type: Int,
            id: Int,
            private val textResId: Int,
            private val drawableResId: Int,
            private val tintResId: Int?
        ) : MenuItem(type, id) {
            override fun createView(context: Context, parent: ViewGroup): View {
                return LayoutInflater.from(context)
                        .inflate(R.layout.menu_item_text_image, parent, false).apply {
                            id = viewId
                            menu_item_image.apply {
                                setImageResource(drawableResId)
                                if (tintResId != null) {
                                    imageTintList = ContextCompat.getColorStateList(context, tintResId)
                                }
                            }
                            menu_item_text.apply {
                                setText(textResId)
                                if (tintResId != null) {
                                    setTextColor(ContextCompat.getColorStateList(context, tintResId))
                                }
                            }
                        }
            }
        }
    }

    companion object {
        const val ROW_CAPACITY = 4
    }
}