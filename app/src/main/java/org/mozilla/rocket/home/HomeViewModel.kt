package org.mozilla.rocket.home

import androidx.lifecycle.ViewModel
import org.mozilla.focus.utils.Settings
import org.mozilla.rocket.download.SingleLiveEvent

class HomeViewModel(
    private val settings: Settings
) : ViewModel() {

    val toggleBackgroundColor = SingleLiveEvent<Unit>()
    val resetBackgroundColor = SingleLiveEvent<Unit>()

    fun onBackgroundViewDoubleTap(): Boolean {
        // Not allowed double tap to switch theme when night mode is on
        if (settings.isNightModeEnable) return false

        toggleBackgroundColor.call()
        return true
    }

    fun onBackgroundViewLongPress() {
        // Not allowed long press to reset theme when night mode is on
        if (settings.isNightModeEnable) return

        resetBackgroundColor.call()
    }

    fun onShoppingButtonClicked() {
        // TODO:
    }
}