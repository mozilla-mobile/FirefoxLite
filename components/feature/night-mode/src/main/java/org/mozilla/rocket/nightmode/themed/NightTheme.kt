/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.rocket.nightmode.themed

interface NightTheme : DefaultTheme {

    fun setNightMode(isNight: Boolean) {
        if (isNightTheme() != isNight) {
            if (isNight) {
                addThemeState(ThemedWidgetUtils.ThemeState.NIGHT)
            } else {
                removeThemeState(ThemedWidgetUtils.ThemeState.NIGHT)
            }
            notifyRefreshDrawableState()
        }
    }

    fun isNightTheme(): Boolean {
        return themeState and ThemedWidgetUtils.ThemeState.NIGHT.value != 0
    }

    override fun getThemeDrawableState(): IntArray {
        return ThemedWidgetUtils.STATE_NIGHT_MODE
    }
}