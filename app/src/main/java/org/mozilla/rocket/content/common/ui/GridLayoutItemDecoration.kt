package org.mozilla.rocket.content.common.ui

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class GridLayoutItemDecoration(private val horizontalSpace: Int, private val verticalSpace: Int, private val spanCount: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildLayoutPosition(view)
        outRect.bottom = verticalSpace
        outRect.right = if (position % spanCount != spanCount - 1) horizontalSpace else 0
    }
}