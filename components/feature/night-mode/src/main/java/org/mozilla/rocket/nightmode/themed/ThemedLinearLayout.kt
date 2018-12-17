/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.rocket.nightmode.themed

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout

class ThemedLinearLayout(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs), NightTheme {

    override var themeState = ThemedWidgetUtils.ThemeState.DEFAULT.value

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        return if (isNightTheme()) {
            val drawableState = super.onCreateDrawableState(extraSpace + getThemeDrawableState().size)
            View.mergeDrawableStates(drawableState, getThemeDrawableState())
            drawableState
        } else {
            super.onCreateDrawableState(extraSpace)
        }
    }

    override fun notifyRefreshDrawableState() {
        refreshDrawableState()
    }
}
