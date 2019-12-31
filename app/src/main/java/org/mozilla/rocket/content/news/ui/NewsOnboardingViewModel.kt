package org.mozilla.rocket.content.news.ui

import androidx.lifecycle.ViewModel
import org.mozilla.rocket.content.news.domain.SetOnboardingHasShownUseCase
import org.mozilla.rocket.content.news.domain.ShouldShowOnboardingUseCase
import org.mozilla.rocket.download.SingleLiveEvent

class NewsOnboardingViewModel(
    private val shouldShowOnboarding: ShouldShowOnboardingUseCase,
    private val setOnboardingHasShown: SetOnboardingHasShownUseCase
) : ViewModel() {
    val showContent = SingleLiveEvent<Content>()

    fun checkContentToShow() {
        showContent.value = if (shouldShowOnboarding()) {
            Content.PersonalizationOnboarding
        } else {
            Content.NewsTab
        }
    }

    fun onPersonalizationSelected() {
        setOnboardingHasShown()
        showContent.value = Content.LanguageOnboarding
    }

    fun onLanguageSelected() {
        showContent.value = Content.NewsTab
    }

    sealed class Content {
        object PersonalizationOnboarding : Content()
        object LanguageOnboarding : Content()
        object NewsTab : Content()
    }
}