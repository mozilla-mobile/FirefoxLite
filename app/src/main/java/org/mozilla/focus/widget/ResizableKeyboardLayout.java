/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import org.mozilla.focus.R;

/**
 * A CoordinatorLayout implementation that resizes dynamically (by adding padding to the bottom)
 * based on whether a keyboard is visible or not.
 * <p>
 * Implementation based on:
 * https://github.com/mikepenz/MaterialDrawer/blob/master/library/src/main/java/com/mikepenz/materialdrawer/util/KeyboardUtil.java
 * <p>
 * An optional viewToHideWhenActivated can be set: this is a View that will be hidden when the keyboard
 * is showing. That can be useful for things like FABs that you don't need when someone is typing.
 */
public class ResizableKeyboardLayout extends CoordinatorLayout {

    private final int idOfViewToHide;

    @Nullable
    private View viewToHide;
    private int marginBottom;

    public ResizableKeyboardLayout(Context context) {
        this(context, null);
    }

    public ResizableKeyboardLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ResizableKeyboardLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final TypedArray styleAttributeArray = getContext().getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.ResizableKeyboardLayout,
                0, 0);

        try {
            idOfViewToHide = styleAttributeArray.getResourceId(R.styleable.ResizableKeyboardLayout_viewToHideWhenActivated, -1);
        } finally {
            styleAttributeArray.recycle();
        }
        this.setOnApplyWindowInsetsListener((v, insets) -> {
            int difference = insets.getSystemWindowInsetBottom();

            if (difference != 0) {
                // Keyboard showing -> Set difference has bottom padding.
                if (getPaddingBottom() != difference) {
                    setPadding(0, 0, 0, difference);
                    // Zerda modification: We don't want extra margin in BrowserFragment for main
                    // toolbar, so we truncate them.
                    if (getLayoutParams() instanceof MarginLayoutParams) {
                        ((MarginLayoutParams) getLayoutParams()).bottomMargin = 0;
                    }

                    if (viewToHide != null) {
                        viewToHide.setVisibility(View.GONE);
                    }
                }
            } else {
                // Keyboard not showing -> Reset bottom padding.
                if (getPaddingBottom() != 0) {
                    setPadding(0, 0, 0, 0);
                    // Zerda modification: Restore previously canceled margin
                    ((MarginLayoutParams) getLayoutParams()).bottomMargin = marginBottom;

                    if (viewToHide != null) {
                        viewToHide.setVisibility(View.VISIBLE);
                    }
                }
            }
            return insets;
        });
    }

    // Zerda modification: Intercept bottomMargin
    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params) {
        super.setLayoutParams(params);
        if (params instanceof MarginLayoutParams) {
            marginBottom = (((MarginLayoutParams) params).bottomMargin);
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (idOfViewToHide != -1) {
            viewToHide = findViewById(idOfViewToHide);
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        viewToHide = null;
    }
}
