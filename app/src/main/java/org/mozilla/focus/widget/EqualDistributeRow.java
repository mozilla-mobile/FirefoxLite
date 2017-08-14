/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * A just-work custom ViewGroup for Bottom Sheet. It layout children in horizontal orientation, and
 * insert equal spacing between each children.
 */
public class EqualDistributeRow extends ViewGroup {

    public EqualDistributeRow(Context context) {
        super(context);
    }

    public EqualDistributeRow(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        int width = calculateWidth(widthMeasureSpec, heightMeasureSpec);
        int height = calculateHeight(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    private int calculateWidth(int widthMeasureSpec, int heightMeasureSpec) {
        final int childCount = getChildCount();
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        if (widthMode == MeasureSpec.EXACTLY) {
            return widthSize;
        }

        int width = 0;

        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            width = width + child.getMeasuredWidth();
        }

        if (widthMode == MeasureSpec.AT_MOST) {
            width = Math.min(width, widthSize);
        }

        return width;
    }

    private int calculateHeight(int widthMeasureSpec, int heightMeasureSpec) {
        final int childCount = getChildCount();
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (heightMode == MeasureSpec.EXACTLY) {
            return heightSize;
        }

        int highest = 0;
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            highest = child.getMeasuredHeight() > highest ? child.getMeasuredHeight() : highest;
        }

        int total = highest + getPaddingTop() + getPaddingBottom();

        if (heightMode == MeasureSpec.AT_MOST) {
            total = Math.min(total, heightSize);
        }

        return total;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int childCount = getChildCount();
        int childrenTotalWidth = 0;
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            childrenTotalWidth += child.getMeasuredWidth();
        }

        final int gutterNum = (childCount > 2) ? childCount - 1 : 0;

        final int drawableWidth = (r - l - childrenTotalWidth) - getPaddingStart() - getPaddingEnd();
        final int spacing = (gutterNum == 0) ? 0 : drawableWidth / gutterNum;

        int x = getPaddingStart();

        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }

            final int childWidth = child.getMeasuredWidth();
            final int childHeight = child.getMeasuredHeight();
            child.layout(x, 0, x + childWidth, childHeight);
            x += childWidth + spacing;
        }
    }
}
