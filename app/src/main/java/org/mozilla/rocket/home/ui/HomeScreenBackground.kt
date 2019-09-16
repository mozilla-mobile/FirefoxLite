/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.home.ui

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import org.mozilla.focus.R
import org.mozilla.focus.utils.ViewUtils
import org.mozilla.rocket.nightmode.themed.ThemedImageView
import org.mozilla.rocket.theme.ThemeManager

class HomeScreenBackground : ThemedImageView, ThemeManager.Themeable {
    private var isNight = false

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs, 0) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    internal fun init() {
        scaleType = ScaleType.CENTER_CROP
        onThemeChanged()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (this.isNight) {
            // Add status bar's height as a padding on top to let HomeFragment star background align with TabTrayFragment
            setPadding(0, ViewUtils.getStatusBarHeight(context as Activity), 0, 0)
        } else {
            setPadding(0, 0, 0, 0)
        }
    }

    override fun setNightMode(isNight: Boolean) {
        super.setNightMode(isNight)
        this.isNight = isNight
        if (this.isNight) {
            setBackgroundResource(R.drawable.bg_homescreen_night_mode)
            setImageResource(R.drawable.star_bg)
        } else {
            setBackgroundColor(Color.TRANSPARENT)
            applyCurrentTheme(context.theme)
        }
    }

    override fun onThemeChanged() {
        applyCurrentTheme(context.theme)
    }

    private fun applyCurrentTheme(theme: Resources.Theme) {
        val typedValue = TypedValue()
        theme.resolveAttribute(R.attr.themeGradientStart, typedValue, true)
        val colorStart = typedValue.data
        theme.resolveAttribute(R.attr.themeGradientCenter, typedValue, true)
        val colorCenter = typedValue.data
        theme.resolveAttribute(R.attr.themeGradientEnd, typedValue, true)
        val colorEnd = typedValue.data

        setImageDrawable(getPatternDrawable(colorStart, colorCenter, colorEnd))
    }

    private fun getPatternDrawable(colorStart: Int, colorCenter: Int, colorEnd: Int): Drawable =
            getScreenedDrawable(
                PATTERN_IMAGE_RESOURCE_ID,
                intArrayOf(colorStart, colorCenter, colorEnd),
                COLORS_GRADIENT_POSITIONS
            )

    private fun getScreenedDrawable(resId: Int, colors: IntArray, positions: FloatArray): Drawable =
            applyColorScreen(
                bitmap = requireNotNull(AppCompatResources.getDrawable(context, resId)).toBitmap(),
                colors = colors,
                positions = positions
            ).toDrawable(resources)

    private fun applyColorScreen(bitmap: Bitmap, colors: IntArray, positions: FloatArray): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val updatedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(updatedBitmap)

        canvas.drawBitmap(bitmap, 0f, 0f, null)

        val paint = Paint()
        val shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), colors, positions, Shader.TileMode.CLAMP)
        paint.shader = shader
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        return updatedBitmap
    }

    companion object {
        private const val PATTERN_IMAGE_RESOURCE_ID = R.drawable.bg_homescreen_pattern
        private val COLORS_GRADIENT_POSITIONS = floatArrayOf(0f, 0.65f, 1f)
    }
}
