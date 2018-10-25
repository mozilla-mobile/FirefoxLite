package org.mozilla.rocket.nightmode.themed

import org.mozilla.rocket.nigtmode.R

object ThemedWidgetUtils {
    val STATE_NIGHT_MODE = intArrayOf(R.attr.state_night)

    enum class ThemeState(val value: Int) {
        DEFAULT(0b000000),
        NIGHT(0b000001);
    }
}
