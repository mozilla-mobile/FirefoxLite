package org.mozilla.rocket.content.news.ui

import androidx.lifecycle.ViewModel
import org.mozilla.rocket.download.SingleLiveEvent

class NewsOnboardingViewModel() : ViewModel() {
    val languageOnboardingDone = SingleLiveEvent<Unit>()

    fun onLanguageSelected() {
        languageOnboardingDone.call()
    }
}
