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