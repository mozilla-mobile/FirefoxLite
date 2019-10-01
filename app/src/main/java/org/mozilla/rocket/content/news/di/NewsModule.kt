package org.mozilla.rocket.content.news.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.mozilla.rocket.content.news.data.NewsRepositoryProvider
import org.mozilla.rocket.content.news.data.NewsSettingsRepositoryProvider
import org.mozilla.rocket.content.news.domain.LoadNewsLanguagesUseCase
import org.mozilla.rocket.content.news.domain.LoadNewsSettingsUseCase
import org.mozilla.rocket.content.news.domain.LoadNewsUseCase
import org.mozilla.rocket.content.news.domain.SetUserPreferenceCategoriesUseCase
import org.mozilla.rocket.content.news.domain.SetUserPreferenceLanguageUseCase
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
        LoadNewsSettingsUseCase(newsSettingsRepositoryProvider)

    @JvmStatic
    @Singleton
    @Provides
    fun provideLoadNewsLanguagesUseCase(newsSettingsRepositoryProvider: NewsSettingsRepositoryProvider): LoadNewsLanguagesUseCase =
        LoadNewsLanguagesUseCase(newsSettingsRepositoryProvider)

    @JvmStatic
    @Singleton
    @Provides
    fun provideSetUserPreferenceLanguageUseCase(newsSettingsRepositoryProvider: NewsSettingsRepositoryProvider): SetUserPreferenceLanguageUseCase =
        SetUserPreferenceLanguageUseCase(newsSettingsRepositoryProvider)

    @JvmStatic
    @Singleton
    @Provides
    fun provideSetUserPreferenceCategoriesUseCase(newsSettingsRepositoryProvider: NewsSettingsRepositoryProvider): SetUserPreferenceCategoriesUseCase =
        SetUserPreferenceCategoriesUseCase(newsSettingsRepositoryProvider)

    @JvmStatic
    @Singleton
    @Provides
    fun provideLoadNewsUseCase(newsRepositoryProvider: NewsRepositoryProvider): LoadNewsUseCase =
        LoadNewsUseCase(newsRepositoryProvider)

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
    @Singleton
    @Provides
    fun provideNewsRepositoryProvider(): NewsRepositoryProvider =
        NewsRepositoryProvider()

    @JvmStatic
    @Singleton
    @Provides
    fun provideNewsSettingsRepositoryProvider(context: Context): NewsSettingsRepositoryProvider =
        NewsSettingsRepositoryProvider(context)
}
