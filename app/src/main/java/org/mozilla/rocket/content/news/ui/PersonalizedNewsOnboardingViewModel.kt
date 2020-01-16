package org.mozilla.rocket.content.news.ui

import androidx.lifecycle.ViewModel
import org.mozilla.rocket.download.SingleLiveEvent

class PersonalizedNewsOnboardingViewModel : ViewModel() {

    val openLearnMorePage = SingleLiveEvent<Unit>()
    val setEnablePersonalizedNews = SingleLiveEvent<Boolean>()

    fun onLearnMoreLinkClick() {
        openLearnMorePage.call()
    }

    fun onPersonalizationSelected(enable: Boolean) {
        setEnablePersonalizedNews.value = enable
    }
}