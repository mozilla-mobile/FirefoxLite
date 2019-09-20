/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.home.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.graphics.drawable.Drawable
import androidx.appcompat.widget.AppCompatImageView
import android.util.AttributeSet
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable

import org.mozilla.focus.R

class PrivateHomeScreenBackground : AppCompatImageView {
    constructor(context: Context) : super(context, null) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs, 0) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    internal fun init(context: Context) {
        scaleType = ScaleType.CENTER_CROP
        applyPrivateTheme(context)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        setPadding(0, 0, 0, 0)
    }

    private fun applyPrivateTheme(context: Context) {
        val colorStart = ContextCompat.getColor(context, R.color.themeGradientStartPrivate)
        val colorCenter = ContextCompat.getColor(context, R.color.themeGradientCenterPrivate)
        val colorEnd = ContextCompat.getColor(context, R.color.themeGradientEndPrivate)

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
