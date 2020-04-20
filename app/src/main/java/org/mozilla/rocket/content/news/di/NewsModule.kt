package org.mozilla.rocket.content.news.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.mozilla.rocket.content.news.data.NewsDataSourceFactory
import org.mozilla.rocket.content.news.data.NewsRepository
import org.mozilla.rocket.content.news.data.NewsSettingsRepositoryProvider
import org.mozilla.rocket.content.news.domain.GetAdditionalSourceInfoUseCase
import org.mozilla.rocket.content.news.domain.HasUserEnabledPersonalizedNewsUseCase
import org.mozilla.rocket.content.news.domain.LoadNewsLanguagesUseCase
import org.mozilla.rocket.content.news.domain.LoadNewsSettingsUseCase
import org.mozilla.rocket.content.news.domain.LoadNewsUseCase
import org.mozilla.rocket.content.news.domain.LoadRawNewsLanguagesUseCase
import org.mozilla.rocket.content.news.domain.SetNewsLanguageSettingPageStateUseCase
import org.mozilla.rocket.content.news.domain.SetPersonalizedNewsOnboardingHasShownUseCase
import org.mozilla.rocket.content.news.domain.SetUserEnabledPersonalizedNewsUseCase
import org.mozilla.rocket.content.news.domain.SetUserPreferenceCategoriesUseCase
import org.mozilla.rocket.content.news.domain.SetUserPreferenceLanguageUseCase
import org.mozilla.rocket.content.news.domain.ShouldEnablePersonalizedNewsUseCase
import org.mozilla.rocket.content.news.domain.ShouldShowNewsLanguageSettingPageUseCase
import org.mozilla.rocket.content.news.domain.ShouldShowPersonalizedNewsOnboardingUseCase
import org.mozilla.rocket.content.news.ui.NewsLanguageSettingViewModel
import org.mozilla.rocket.content.news.ui.NewsPageStateViewModel
import org.mozilla.rocket.content.news.ui.NewsSettingsViewModel
import org.mozilla.rocket.content.news.ui.NewsTabViewModel
import org.mozilla.rocket.content.news.ui.NewsViewModel
import org.mozilla.rocket.content.news.ui.PersonalizedNewsOnboardingViewModel
import javax.inject.Singleton

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
    fun provideLoadNewsUseCase(
        newsRepository: NewsRepository
    ): LoadNewsUseCase = LoadNewsUseCase(newsRepository)

    @JvmStatic
    @Provides
    fun provideGetAdditonalSourceInfoUseCase(newsSettingsRepositoryProvider: NewsSettingsRepositoryProvider): GetAdditionalSourceInfoUseCase =
        GetAdditionalSourceInfoUseCase(newsSettingsRepositoryProvider.provideNewsSettingsRepository())

    @JvmStatic
    @Provides
    fun provideSetUserEnabledPersonalizedNewsUseCase(newsSettingsRepositoryProvider: NewsSettingsRepositoryProvider): SetUserEnabledPersonalizedNewsUseCase =
        SetUserEnabledPersonalizedNewsUseCase(newsSettingsRepositoryProvider.provideNewsSettingsRepository())

    @JvmStatic
    @Provides
    fun provideShouldUserEnabledPersonalizedNewsUseCase(newsSettingsRepositoryProvider: NewsSettingsRepositoryProvider): HasUserEnabledPersonalizedNewsUseCase =
        HasUserEnabledPersonalizedNewsUseCase(newsSettingsRepositoryProvider.provideNewsSettingsRepository())

    @JvmStatic
    @Provides
    fun provideShouldEnablePersonalizedNewsUseCase(newsSettingsRepositoryProvider: NewsSettingsRepositoryProvider): ShouldEnablePersonalizedNewsUseCase =
        ShouldEnablePersonalizedNewsUseCase(newsSettingsRepositoryProvider.provideNewsSettingsRepository())

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
    fun provideShouldShowNewsLanguageSettingPageUseCase(newsSettingsRepositoryProvider: NewsSettingsRepositoryProvider): ShouldShowNewsLanguageSettingPageUseCase =
        ShouldShowNewsLanguageSettingPageUseCase(newsSettingsRepositoryProvider.provideNewsSettingsRepository())

    @JvmStatic
    @Provides
    fun provideSetNewsLanguageSettingPageStateUseCase(newsSettingsRepositoryProvider: NewsSettingsRepositoryProvider): SetNewsLanguageSettingPageStateUseCase =
        SetNewsLanguageSettingPageStateUseCase(newsSettingsRepositoryProvider.provideNewsSettingsRepository())

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
        setUserPreferenceCategoriesUseCase: SetUserPreferenceCategoriesUseCase,
        shouldEnablePersonalizedNewsUseCase: ShouldEnablePersonalizedNewsUseCase,
        hasUserEnabledPersonalizedNewsUseCase: HasUserEnabledPersonalizedNewsUseCase,
        setUserEnabledPersonalizedNewsUseCase: SetUserEnabledPersonalizedNewsUseCase,
        setNewsLanguageSettingPageStateUseCase: SetNewsLanguageSettingPageStateUseCase
    ): NewsSettingsViewModel =
        NewsSettingsViewModel(
            loadNewsSettingsUseCase,
            loadNewsLanguagesUseCase,
            setUserPreferenceLanguageUseCase,
            setUserPreferenceCategoriesUseCase,
            shouldEnablePersonalizedNewsUseCase,
            hasUserEnabledPersonalizedNewsUseCase,
            setUserEnabledPersonalizedNewsUseCase,
            setNewsLanguageSettingPageStateUseCase
        )

    @JvmStatic
    @Provides
    fun provideNewsPageStateViewModel(
        shouldShowPersonalizedNewsOnboardingUseCase: ShouldShowPersonalizedNewsOnboardingUseCase,
        setPersonalizedNewsOnboardingHasShownUseCase: SetPersonalizedNewsOnboardingHasShownUseCase,
        shouldShowNewsLanguageSettingPageUseCase: ShouldShowNewsLanguageSettingPageUseCase,
        setNewsLanguageSettingPageStateUseCase: SetNewsLanguageSettingPageStateUseCase,
        setUserEnabledPersonalizedNewsUseCase: SetUserEnabledPersonalizedNewsUseCase
    ): NewsPageStateViewModel =
        NewsPageStateViewModel(
            shouldShowPersonalizedNewsOnboardingUseCase,
            setPersonalizedNewsOnboardingHasShownUseCase,
            shouldShowNewsLanguageSettingPageUseCase,
            setNewsLanguageSettingPageStateUseCase,
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

    @Singleton // Single instance to persist same dataSourceFactory for LoadNewsUseCase and RefreshNewsUseCase usages
    @JvmStatic
    @Provides
    fun provideNewsRepository(dataSourceFactory: NewsDataSourceFactory): NewsRepository =
            NewsRepository(dataSourceFactory)

    @JvmStatic
    @Provides
    fun provideNewsDataSourceFactory(context: Context): NewsDataSourceFactory =
            NewsDataSourceFactory(context)

    @JvmStatic
    @Provides
    fun provideNewsSettingsRepositoryProvider(context: Context): NewsSettingsRepositoryProvider =
        NewsSettingsRepositoryProvider(context)
}
