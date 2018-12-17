/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.rocket.nightmode.themed

interface DefaultTheme {
    var themeState: Int

    fun notifyRefreshDrawableState()

    fun addThemeState(state: ThemedWidgetUtils.ThemeState) {
        themeState = themeState or state.value
    }

    fun removeThemeState(state: ThemedWidgetUtils.ThemeState) {
        themeState = themeState and state.value.inv()
    }

    fun getThemeDrawableState(): IntArray
}