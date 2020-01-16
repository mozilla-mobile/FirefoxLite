package org.mozilla.rocket.content.news.ui

import androidx.lifecycle.ViewModel
import org.mozilla.rocket.content.news.domain.SetPersonalizedNewsOnboardingHasShownUseCase
import org.mozilla.rocket.content.news.domain.SetUserEnabledPersonalizedNewsUseCase
import org.mozilla.rocket.content.news.domain.ShouldShowPersonalizedNewsOnboardingUseCase
import org.mozilla.rocket.download.SingleLiveEvent

class NewsPageStateViewModel(
    private val shouldShowPersonalizedNewsOnboarding: ShouldShowPersonalizedNewsOnboardingUseCase,
    private val setPersonalizedNewsOnboardingHasShown: SetPersonalizedNewsOnboardingHasShownUseCase,
    private val setUserEnabledPersonalizedNews: SetUserEnabledPersonalizedNewsUseCase
) : ViewModel() {
    val showContent = SingleLiveEvent<Page>()

    fun checkPageToShow() {
        showContent.value = if (shouldShowPersonalizedNewsOnboarding()) {
            Page.PersonalizationOnboarding
        } else {
            Page.NewsContent
        }
    }

    fun onPersonalizedOptionSelected(enable: Boolean) {
        setUserEnabledPersonalizedNews(enable)
        setPersonalizedNewsOnboardingHasShown()
        showContent.value = Page.LanguageSetting
    }

    fun onLanguageSelected() {
        showContent.value = Page.NewsContent
    }

    sealed class Page {
        object PersonalizationOnboarding : Page()
        object LanguageSetting : Page()
        object NewsContent : Page()
    }
}