package org.mozilla.rocket.content.view;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class SpacingDecoration extends RecyclerView.ItemDecoration {
    private int spacing;

    public SpacingDecoration(int spacing) {
        this.spacing = spacing;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        outRect.set(spacing, spacing, spacing, spacing);
    }
}
