package org.mozilla.rocket.content.games.ui

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class GameItemDecoration(val paddingLR: Int, val padding: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)

        val itemPosition = parent.getChildAdapterPosition(view)
        if (itemPosition == RecyclerView.NO_POSITION) {
            return
        }

        val itemCount = state.itemCount

        /* first position  */
        if (itemPosition == 0) {
            outRect.set(paddingLR, padding, padding, padding)
        /* last position  */
        } else if (itemCount > 0 && itemPosition == itemCount - 1) {
            outRect.set(padding, padding, paddingLR, padding)
        /* positions between first and last  */
        } else {
            outRect.set(padding, padding, padding, padding)
        }
    }
}