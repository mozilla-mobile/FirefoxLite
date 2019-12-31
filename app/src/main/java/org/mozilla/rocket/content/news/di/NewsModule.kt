package org.mozilla.rocket.content.news.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.mozilla.rocket.content.news.data.NewsOnboardingRepository
import org.mozilla.rocket.content.news.data.NewsRepositoryProvider
import org.mozilla.rocket.content.news.data.NewsSettingsRepositoryProvider
import org.mozilla.rocket.content.news.domain.LoadNewsLanguagesUseCase
import org.mozilla.rocket.content.news.domain.LoadNewsSettingsUseCase
import org.mozilla.rocket.content.news.domain.LoadNewsUseCase
import org.mozilla.rocket.content.news.domain.SetOnboardingHasShownUseCase
import org.mozilla.rocket.content.news.domain.SetUserPreferenceCategoriesUseCase
import org.mozilla.rocket.content.news.domain.SetUserPreferenceLanguageUseCase
import org.mozilla.rocket.content.news.domain.ShouldShowOnboardingUseCase
import org.mozilla.rocket.content.news.ui.NewsLanguageOnboardingViewModel
import org.mozilla.rocket.content.news.ui.NewsOnboardingViewModel
import org.mozilla.rocket.content.news.ui.NewsPersonalizationOnboardingViewModel
import org.mozilla.rocket.content.news.ui.NewsSettingsViewModel
import org.mozilla.rocket.content.news.ui.NewsTabViewModel
import org.mozilla.rocket.content.news.ui.NewsViewModel
import javax.inject.Singleton

@Module
object NewsModule {

    @JvmStatic
    @Singleton
    @Provides
    fun provideLoadNewsSettingsUseCase(newsSettingsRepositoryProvider: NewsSettingsRepositoryProvider): LoadNewsSettingsUseCase =
        LoadNewsSettingsUseCase(newsSettingsRepositoryProvider.provideNewsSettingsRepository())

    @JvmStatic
    @Singleton
    @Provides
    fun provideLoadNewsLanguagesUseCase(newsSettingsRepositoryProvider: NewsSettingsRepositoryProvider): LoadNewsLanguagesUseCase =
        LoadNewsLanguagesUseCase(newsSettingsRepositoryProvider.provideNewsSettingsRepository())

    @JvmStatic
    @Singleton
    @Provides
    fun provideSetUserPreferenceLanguageUseCase(newsSettingsRepositoryProvider: NewsSettingsRepositoryProvider): SetUserPreferenceLanguageUseCase =
        SetUserPreferenceLanguageUseCase(newsSettingsRepositoryProvider.provideNewsSettingsRepository())

    @JvmStatic
    @Singleton
    @Provides
    fun provideSetUserPreferenceCategoriesUseCase(newsSettingsRepositoryProvider: NewsSettingsRepositoryProvider): SetUserPreferenceCategoriesUseCase =
        SetUserPreferenceCategoriesUseCase(newsSettingsRepositoryProvider.provideNewsSettingsRepository())

    @JvmStatic
    @Singleton
    @Provides
    fun provideLoadNewsUseCase(newsRepositoryProvider: NewsRepositoryProvider): LoadNewsUseCase =
        LoadNewsUseCase(newsRepositoryProvider.provideNewsRepository())

    @JvmStatic
    @Singleton
    @Provides
    fun provideShouldShowOnboardingUseCase(newsOnboardingRepository: NewsOnboardingRepository): ShouldShowOnboardingUseCase = ShouldShowOnboardingUseCase(newsOnboardingRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideSetOnboardingHasShownUseCase(newsOnboardingRepository: NewsOnboardingRepository): SetOnboardingHasShownUseCase = SetOnboardingHasShownUseCase(newsOnboardingRepository)

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
    fun provideNewsOnboardingViewModel(
        shouldShowOnboardingUseCase: ShouldShowOnboardingUseCase,
        setOnboardingHasShownUseCase: SetOnboardingHasShownUseCase
    ): NewsOnboardingViewModel =
        NewsOnboardingViewModel(shouldShowOnboardingUseCase, setOnboardingHasShownUseCase)

    @JvmStatic
    @Provides
    fun provideNewsPersonalizationOnboardingViewModel(): NewsPersonalizationOnboardingViewModel =
        NewsPersonalizationOnboardingViewModel()

    @JvmStatic
    @Provides
    fun provideNewsLanguageOnboardingViewModel(
        loadNewsLanguagesUseCase: LoadNewsLanguagesUseCase,
        setUserPreferenceLanguageUseCase: SetUserPreferenceLanguageUseCase
    ): NewsLanguageOnboardingViewModel =
        NewsLanguageOnboardingViewModel(loadNewsLanguagesUseCase, setUserPreferenceLanguageUseCase)

    @JvmStatic
    @Singleton
    @Provides
    fun provideNewsRepositoryProvider(): NewsRepositoryProvider =
        NewsRepositoryProvider()

    @JvmStatic
    @Singleton
    @Provides
    fun provideNewsSettingsRepositoryProvider(context: Context): NewsSettingsRepositoryProvider =
        NewsSettingsRepositoryProvider(context)

    @JvmStatic
    @Singleton
    @Provides
    fun provideNewsOnboardingRepository(
        appContext: Context
    ): NewsOnboardingRepository = NewsOnboardingRepository(appContext)
}
