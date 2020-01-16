package org.mozilla.rocket.content.news.ui

import androidx.lifecycle.ViewModel
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.content.news.domain.SetNewsLanguageSettingPageStateUseCase
import org.mozilla.rocket.content.news.domain.SetPersonalizedNewsOnboardingHasShownUseCase
import org.mozilla.rocket.content.news.domain.SetUserEnabledPersonalizedNewsUseCase
import org.mozilla.rocket.content.news.domain.ShouldShowNewsLanguageSettingPageUseCase
import org.mozilla.rocket.content.news.domain.ShouldShowPersonalizedNewsOnboardingUseCase
import org.mozilla.rocket.download.SingleLiveEvent

class NewsPageStateViewModel(
    private val shouldShowPersonalizedNewsOnboarding: ShouldShowPersonalizedNewsOnboardingUseCase,
    private val setPersonalizedNewsOnboardingHasShown: SetPersonalizedNewsOnboardingHasShownUseCase,
    private val shouldShowNewsLanguageSettingPage: ShouldShowNewsLanguageSettingPageUseCase,
    private val setNewsLanguageSettingPageState: SetNewsLanguageSettingPageStateUseCase,
    private val setUserEnabledPersonalizedNews: SetUserEnabledPersonalizedNewsUseCase
) : ViewModel() {
    val showContent = SingleLiveEvent<Page>()

    fun checkPageToShow() {
        showContent.value = when {
            shouldShowPersonalizedNewsOnboarding() -> {
                TelemetryWrapper.showVerticalOnboarding(TelemetryWrapper.Extra_Value.LIFESTYLE)
                Page.PersonalizationOnboarding
            }
            shouldShowNewsLanguageSettingPage() -> {
                Page.LanguageSetting
            }
            else -> {
                Page.NewsContent
            }
        }
    }

    fun onPersonalizedOptionSelected(enable: Boolean) {
        setUserEnabledPersonalizedNews(enable)
        setPersonalizedNewsOnboardingHasShown()
        showContent.value = Page.LanguageSetting
        TelemetryWrapper.changePersonalizationInOnboarding(TelemetryWrapper.Extra_Value.LIFESTYLE, enable)
    }

    fun onLanguageSelected(language: String) {
        setNewsLanguageSettingPageState(false)
        showContent.value = Page.NewsContent
        TelemetryWrapper.changeLanguageInOnboarding(TelemetryWrapper.Extra_Value.LIFESTYLE, language)
    }

    sealed class Page {
        object PersonalizationOnboarding : Page()
        object LanguageSetting : Page()
        object NewsContent : Page()
    }
}