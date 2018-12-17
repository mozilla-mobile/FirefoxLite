/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.rocket.nightmode.themed

import org.mozilla.rocket.nightmode.R

object ThemedWidgetUtils {
    val STATE_NIGHT_MODE = intArrayOf(R.attr.state_night)

    enum class ThemeState(val value: Int) {
        DEFAULT(0b000000),
        NIGHT(0b000001);
    }
}
