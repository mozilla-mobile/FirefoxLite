package org.mozilla.rocket.content.news.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.mozilla.rocket.content.news.data.NewsRepositoryProvider
import org.mozilla.rocket.content.news.data.NewsSettingsRepositoryProvider
import org.mozilla.rocket.content.news.domain.LoadNewsLanguagesUseCase
import org.mozilla.rocket.content.news.domain.LoadNewsSettingsUseCase
import org.mozilla.rocket.content.news.domain.LoadNewsUseCase
import org.mozilla.rocket.content.news.domain.LoadRawNewsLanguagesUseCase
import org.mozilla.rocket.content.news.domain.SetPersonalizedNewsOnboardingHasShownUseCase
import org.mozilla.rocket.content.news.domain.SetUserEnabledPersonalizedNewsUseCase
import org.mozilla.rocket.content.news.domain.SetUserPreferenceCategoriesUseCase
import org.mozilla.rocket.content.news.domain.SetUserPreferenceLanguageUseCase
import org.mozilla.rocket.content.news.domain.ShouldShowPersonalizedNewsOnboardingUseCase
import org.mozilla.rocket.content.news.ui.NewsLanguageSettingViewModel
import org.mozilla.rocket.content.news.ui.NewsPageStateViewModel
import org.mozilla.rocket.content.news.ui.NewsSettingsViewModel
import org.mozilla.rocket.content.news.ui.NewsTabViewModel
import org.mozilla.rocket.content.news.ui.NewsViewModel
import org.mozilla.rocket.content.news.ui.PersonalizedNewsOnboardingViewModel

@Module
object NewsModule {

    @JvmStatic
    @Provides
    fun provideLoadNewsSettingsUseCase(newsSettingsRepositoryProvider: NewsSettingsRepositoryProvider): LoadNewsSettingsUseCase =
        LoadNewsSettingsUseCase(newsSettingsRepositoryProvider.provideNewsSettingsRepository())

    @JvmStatic
    @Provides
    fun provideLoadNewsLanguagesUseCase(newsSettingsRepositoryProvider: NewsSettingsRepositoryProvider): LoadNewsLanguagesUseCase =
        LoadNewsLanguagesUseCase(newsSettingsRepositoryProvider.provideNewsSettingsRepository())

    @JvmStatic
    @Provides
    fun provideLoadRawNewsLanguagesUseCase(newsSettingsRepositoryProvider: NewsSettingsRepositoryProvider): LoadRawNewsLanguagesUseCase =
        LoadRawNewsLanguagesUseCase(newsSettingsRepositoryProvider.provideNewsSettingsRepository())

    @JvmStatic
    @Provides
    fun provideSetUserPreferenceLanguageUseCase(newsSettingsRepositoryProvider: NewsSettingsRepositoryProvider): SetUserPreferenceLanguageUseCase =
        SetUserPreferenceLanguageUseCase(newsSettingsRepositoryProvider.provideNewsSettingsRepository())

    @JvmStatic
    @Provides
    fun provideSetUserPreferenceCategoriesUseCase(newsSettingsRepositoryProvider: NewsSettingsRepositoryProvider): SetUserPreferenceCategoriesUseCase =
        SetUserPreferenceCategoriesUseCase(newsSettingsRepositoryProvider.provideNewsSettingsRepository())

    @JvmStatic
    @Provides
    fun provideLoadNewsUseCase(newsRepositoryProvider: NewsRepositoryProvider): LoadNewsUseCase =
        LoadNewsUseCase(newsRepositoryProvider.provideNewsRepository())

    @JvmStatic
    @Provides
    fun provideSetUserEnabledPersonalizedNewsUseCase(newsSettingsRepositoryProvider: NewsSettingsRepositoryProvider): SetUserEnabledPersonalizedNewsUseCase =
        SetUserEnabledPersonalizedNewsUseCase(newsSettingsRepositoryProvider.provideNewsSettingsRepository())

    @JvmStatic
    @Provides
    fun provideShouldShowPersonalizedNewsOnboardingUseCase(newsSettingsRepositoryProvider: NewsSettingsRepositoryProvider): ShouldShowPersonalizedNewsOnboardingUseCase =
        ShouldShowPersonalizedNewsOnboardingUseCase(newsSettingsRepositoryProvider.provideNewsSettingsRepository())

    @JvmStatic
    @Provides
    fun provideSetPersonalizedNewsOnboardingHasShownUseCase(newsSettingsRepositoryProvider: NewsSettingsRepositoryProvider): SetPersonalizedNewsOnboardingHasShownUseCase =
        SetPersonalizedNewsOnboardingHasShownUseCase(newsSettingsRepositoryProvider.provideNewsSettingsRepository())

    @JvmStatic
    @Provides
    fun provideNewsViewModel(loadNews: LoadNewsUseCase): NewsViewModel =
        NewsViewModel(loadNews)

    @JvmStatic
    @Provides
    fun provideNewsTabViewModel(loadNewsSettingsUseCase: LoadNewsSettingsUseCase): NewsTabViewModel =
        NewsTabViewModel(loadNewsSettingsUseCase)

    @JvmStatic
    @Provides
    fun provideNewsSettingsViewModel(
        loadNewsSettingsUseCase: LoadNewsSettingsUseCase,
        loadNewsLanguagesUseCase: LoadNewsLanguagesUseCase,
        setUserPreferenceLanguageUseCase: SetUserPreferenceLanguageUseCase,
        setUserPreferenceCategoriesUseCase: SetUserPreferenceCategoriesUseCase
    ): NewsSettingsViewModel =
        NewsSettingsViewModel(loadNewsSettingsUseCase, loadNewsLanguagesUseCase, setUserPreferenceLanguageUseCase, setUserPreferenceCategoriesUseCase)

    @JvmStatic
    @Provides
    fun provideNewsPageStateViewModel(
        shouldShowPersonalizedNewsOnboardingUseCase: ShouldShowPersonalizedNewsOnboardingUseCase,
        setPersonalizedNewsOnboardingHasShownUseCase: SetPersonalizedNewsOnboardingHasShownUseCase,
        setUserEnabledPersonalizedNewsUseCase: SetUserEnabledPersonalizedNewsUseCase
    ): NewsPageStateViewModel =
        NewsPageStateViewModel(
            shouldShowPersonalizedNewsOnboardingUseCase,
            setPersonalizedNewsOnboardingHasShownUseCase,
            setUserEnabledPersonalizedNewsUseCase
        )

    @JvmStatic
    @Provides
    fun providePersonalizedNewsOnboardingViewModel(): PersonalizedNewsOnboardingViewModel =
        PersonalizedNewsOnboardingViewModel()

    @JvmStatic
    @Provides
    fun provideNewsLanguageSettingViewModel(
        loadRawNewsLanguagesUseCase: LoadRawNewsLanguagesUseCase,
        setUserPreferenceLanguageUseCase: SetUserPreferenceLanguageUseCase
    ): NewsLanguageSettingViewModel =
        NewsLanguageSettingViewModel(loadRawNewsLanguagesUseCase, setUserPreferenceLanguageUseCase)

    @JvmStatic
    @Provides
    fun provideNewsRepositoryProvider(context: Context): NewsRepositoryProvider =
        NewsRepositoryProvider(context)

    @JvmStatic
    @Provides
    fun provideNewsSettingsRepositoryProvider(context: Context): NewsSettingsRepositoryProvider =
        NewsSettingsRepositoryProvider(context)
}
