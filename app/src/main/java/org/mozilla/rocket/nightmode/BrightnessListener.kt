package org.mozilla.rocket.nightmode

import android.view.View
import org.mozilla.rocket.nightmode.AdjustBrightnessDialog.Constants.BRIGHT_PERCENTAGE

interface BrightnessListener {

    fun getNightModeCover(): View?

    fun adjustBrightness() {
        val background = getNightModeCover()?.background ?: return
        background.alpha = 255 * BRIGHT_PERCENTAGE / 100
    }
}