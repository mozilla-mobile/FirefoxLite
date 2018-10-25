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