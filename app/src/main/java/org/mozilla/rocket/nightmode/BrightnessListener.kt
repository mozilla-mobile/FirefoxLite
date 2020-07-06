package org.mozilla.rocket.nightmode

import android.view.View

interface BrightnessListener {
    fun adjustBrightness(nightModeCover: View)
}