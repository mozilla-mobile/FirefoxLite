package org.mozilla.rocket.content.common.ui

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class VerticalSpaceItemDecoration(private val spaceHeight: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val layoutManager = parent.layoutManager as LinearLayoutManager
        val position = parent.getChildLayoutPosition(view)
        outRect.top = if (position == 0) 0 else spaceHeight
        outRect.bottom = if (position == layoutManager.itemCount - 1) 0 else spaceHeight
    }
}