/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import org.mozilla.focus.R;

import javax.annotation.Nullable;

/**
 * A just-work custom ViewGroup for UI requirement. It *ASSUME* each children has the same width and
 * height, then to layout children in horizontal orientation by inserting equal spacing between each
 * children. If children number is more than row-capacity(specified in XML), rest children will move
 * to next row, and align previous row.
 * <p>
 * ie: put 12 cells in this ViewGroup with row-capacity = 5, it will look like this
 * [C C C C C]
 * [C C C C C]
 * [C C      ]
 */
public class EqualDistributeGrid extends ViewGroup {

    private int rowCapacity = Integer.MAX_VALUE;
    private int gutter = 0;

    public EqualDistributeGrid(Context context) {
        super(context);
    }

    public EqualDistributeGrid(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.EqualDistributeGrid);
        rowCapacity = a.getInt(R.styleable.EqualDistributeGrid_horizontalCapacity, Integer.MAX_VALUE);
        gutter = a.getDimensionPixelSize(R.styleable.EqualDistributeGrid_verticalGutter, 0);
        a.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChildren(widthMeasureSpec, heightMeasureSpec);

        final int cellCount = getNotGoneCount();

        // how many cells in first row
        final int rowLength = Math.min(rowCapacity, cellCount);

        // to know width and height for each cell
        final View reference = getFirstNotGoneChild();

        int width = calculateWidth(widthMeasureSpec, heightMeasureSpec, cellCount, rowLength, reference);
        int height = calculateHeight(widthMeasureSpec, heightMeasureSpec, cellCount, rowLength, reference);
        setMeasuredDimension(width, height);
    }

    private int calculateWidth(int widthMeasureSpec,
                               int heightMeasureSpec,
                               int cellCount,
                               int rowLength,
                               @Nullable View reference) {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        if (widthMode == MeasureSpec.EXACTLY) {
            return widthSize;
        }

        int width = (reference == null || cellCount == 0)
                ? 0
                : reference.getMeasuredWidth() * rowLength;

        if (widthMode == MeasureSpec.AT_MOST) {
            width = Math.min(width, widthSize);
        }

        return width;
    }

    private int calculateHeight(int widthMeasureSpec,
                                int heightMeasureSpec,
                                int cellCount,
                                int rowLength,
                                @Nullable View reference) {
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (heightMode == MeasureSpec.EXACTLY) {
            return heightSize;
        }

        if (rowLength == 0) {
            return 0;
        }

        int lines = (cellCount / rowLength) + (cellCount % rowLength == 0 ? 0 : 1);

        final int cellsOccupied = (reference == null || cellCount == 0)
                ? 0
                : lines * reference.getMeasuredHeight();

        int height = (reference == null || cellCount == 0)
                ? 0
                : cellsOccupied + (lines - 1) * gutter + getPaddingTop() + getPaddingBottom();

        if (heightMode == MeasureSpec.AT_MOST) {
            height = Math.min(height, heightSize);
        }

        return height;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int cellCount = getNotGoneCount();

        // to know width and height for each cell
        final View reference = getFirstNotGoneChild();

        if (reference == null || cellCount == 0) {
            return;
        }

        final int cellWidth = reference.getMeasuredWidth();
        final int cellHeight = reference.getMeasuredHeight();
        final int paddingStart = getPaddingStart();
        final int paddingEnd = getPaddingEnd();

        // how many cells in first row
        int rowLength = Math.min(rowCapacity, cellCount);

        // space between cells in horizontal orientation
        int spacing = (rowLength == 1)
                ? 0
                : ((r - l) - (cellWidth * rowLength) - paddingStart - paddingEnd) / (rowLength - 1);

        final int childCount = getChildCount();
        int index = 0;
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }

            int x = index % rowLength;
            int y = index / rowLength;

            int locationX = x * cellWidth + spacing * x + paddingStart;
            int locationY = y * cellHeight + gutter * y;

            child.layout(locationX, locationY, locationX + cellWidth, locationY + cellHeight);
            index++;
        }
    }

    private int getNotGoneCount() {
        final int childCnt = getChildCount();
        int count = 0;
        for (int i = 0; i < childCnt; i++) {
            if (getChildAt(i).getVisibility() != GONE) {
                count++;
            }
        }
        return count;
    }

    private View getFirstNotGoneChild() {
        final int childCnt = getChildCount();
        for (int i = 0; i < childCnt; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                return child;
            }
        }
        return null;
    }
}
