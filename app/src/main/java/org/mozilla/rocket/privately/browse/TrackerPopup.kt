/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.privately.browse

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import kotlinx.android.synthetic.main.view_tracker_popup.view.*
import org.mozilla.focus.R

class TrackerPopup(context: Context) : PopupWindow(
        View.inflate(
                context,
                R.layout.view_tracker_popup,
                null
        ),
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        true
) {

    companion object {
        private const val COUNT_INFINITY = "∞"
        private const val COUNT_DISABLED = "-"

        private const val WIDTH_TO_PARENT_WIDTH = 0.8f
        private const val MAX_NUMBER_OF_DIGITS = 2
    }

    private val counterColorEnabled = ContextCompat.getColor(context, R.color.palettePurple100)
    private val counterColorDisabled = ContextCompat.getColor(context, R.color.paletteWhite100)

    var onSwitchToggled: ((Boolean) -> Unit)? = null

    var blockedCount: Int = 0
        set(value) {
            if (contentView.tracker_switch.isChecked) {
                val count = value.toString()
                contentView.tracker_count.text = if (count.length <= MAX_NUMBER_OF_DIGITS) {
                    value.toString()
                } else {
                    COUNT_INFINITY
                }
            }
            field = value
        }

    init {
        val view = contentView
        elevation = contentView.elevation
        setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.background_tracker_popup))

        val switchView = contentView.tracker_switch
        DrawableCompat.setTintList(
                switchView.thumbDrawable,
                ContextCompat.getColorStateList(context, R.color.switch_thumb_dark)
        )
        DrawableCompat.setTintList(
                switchView.trackDrawable,
                ContextCompat.getColorStateList(context, R.color.switch_track_dark)
        )

        val counterBkg = DrawableCompat.wrap(view.tracker_count_container.background).mutate()
        view.tracker_count_container.background = counterBkg

        val counterView = view.tracker_count
        switchView.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                counterView.text = "0"
                DrawableCompat.setTint(counterBkg, counterColorEnabled)
            } else {
                view.tracker_count.text = COUNT_DISABLED
                DrawableCompat.setTint(counterBkg, counterColorDisabled)
            }

            onSwitchToggled?.invoke(isChecked)
        }
    }

    fun setSwitchToggled(enabled: Boolean) {
        contentView.tracker_switch.isChecked = enabled
    }

    fun show(parentView: View) {
        val res = parentView.resources
        val margin: Int = (res.getDimension(R.dimen.fixed_menu_height) +
                res.getDimension(R.dimen.browser_tracker_popup_bottom_margin)).toInt()

        this.width = (parentView.measuredWidth * WIDTH_TO_PARENT_WIDTH).toInt()

        val offsetY = (parentView.context as Activity).window.decorView.let { decorView ->
            val contentHeight = Rect().apply {
                decorView.getWindowVisibleDisplayFrame(this)
            }.bottom
            val winHeight = decorView.height
            winHeight - contentHeight
        }

        showAtLocation(parentView, Gravity.BOTTOM, 0, margin + offsetY)
    }
}