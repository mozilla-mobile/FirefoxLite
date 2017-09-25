/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.screenshot;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import static org.mozilla.focus.screenshot.ScreenshotItemAdapter.VIEW_TYPE_SCREENSHOT;

public class ItemOffsetDecoration extends RecyclerView.ItemDecoration {

    private int spanCount;
    private int spacing;

    public ItemOffsetDecoration(int spanCount, int spacing) {
        this.spanCount = spanCount;
        this.spacing = spacing;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                               RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        int position = parent.getChildAdapterPosition(view); // item position
        if(position >= 0 && parent.getAdapter().getItemViewType(position) == VIEW_TYPE_SCREENSHOT) {
            int adjustPosition = ((ScreenshotItemAdapter)parent.getAdapter()).getAdjustPosition(position);
            int column = adjustPosition % spanCount; // item column

            outRect.left = spacing - column * spacing / spanCount;
            outRect.right = (column + 1) * spacing / spanCount;

            if (adjustPosition < spanCount) { // top edge
                outRect.top = spacing;
            }
            outRect.bottom = spacing; // item bottom
        }
    }
}