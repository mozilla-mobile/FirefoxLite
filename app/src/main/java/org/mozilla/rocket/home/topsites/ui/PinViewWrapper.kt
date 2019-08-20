/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.home.topsites.ui

import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.DrawableCompat
import android.view.View
import android.view.ViewGroup
import org.mozilla.focus.R

class PinViewWrapper(val view: ViewGroup) {
    companion object {
        private const val COLOR_DEFAULT = Color.WHITE
        private const val COLOR_CONTRAST = Color.BLACK

        private const val MIN_CONTRAST_RATIO = 1.5
    }

    var visibility: Int
        set(value) {
            view.visibility = value
        }
        get() {
            return view.visibility
        }

    fun setPinColor(color: Int) {
        // Background
        view.background = view.background?.tint(color)

        // Foreground
        val contrast = ColorUtils.calculateContrast(color, COLOR_DEFAULT)
        val pinIconColor = if (contrast < MIN_CONTRAST_RATIO) {
            COLOR_CONTRAST
        } else {
            COLOR_DEFAULT
        }

        val iconView = view.findViewById<View>(R.id.pin_icon)
        iconView.background = iconView.background?.tint(pinIconColor)
    }

    fun Drawable.tint(color: Int): Drawable {
        return DrawableCompat.wrap(this).mutate().apply {
            DrawableCompat.setTint(this, color)
        }
    }
}
