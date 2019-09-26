package org.mozilla.rocket.content.ecommerce.ui

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class GridSpaceItemDecoration(private val spaceWidth: Int, private val spanCount: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildLayoutPosition(view)
        val column = position % spanCount

        outRect.top = if (position >= spanCount) spaceWidth else 0
        outRect.bottom = spaceWidth
        outRect.left = if (column == 0) 0 else spaceWidth
        outRect.right = if (column == spanCount - 1) 0 else spaceWidth
    }
}