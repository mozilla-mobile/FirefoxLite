package org.mozilla.rocket.content.news.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.mozilla.lite.newspoint.RepositoryNewsPoint
import org.mozilla.lite.partner.NewsItem
import org.mozilla.lite.partner.Repository
import org.mozilla.rocket.content.news.data.NewsRepository
import org.mozilla.rocket.content.news.data.NewsRepositoryProvider
import org.mozilla.rocket.content.news.data.NewsSettingsLocalDataSource
import org.mozilla.rocket.content.news.data.NewsSettingsRemoteDataSource
import org.mozilla.rocket.content.news.data.NewsSettingsRepository
import org.mozilla.rocket.content.news.domain.LoadNewsLanguagesUseCase
import org.mozilla.rocket.content.news.domain.LoadNewsSettingsUseCase
import org.mozilla.rocket.content.news.domain.LoadNewsUseCase
import org.mozilla.rocket.content.news.domain.SetUserPreferenceCategoriesUseCase
import org.mozilla.rocket.content.news.domain.SetUserPreferenceLanguageUseCase
import org.mozilla.rocket.content.news.ui.NewsSettingsViewModel
import org.mozilla.rocket.content.news.ui.NewsTabViewModel
import org.mozilla.rocket.content.news.ui.NewsViewModel
import java.util.HashMap
import java.util.Locale
import javax.inject.Singleton

@Module
object NewsModule {

    @JvmStatic
    @Singleton
    @Provides
    fun provideNewsSettingsRemoteDataSource(): NewsSettingsRemoteDataSource =
        NewsSettingsRemoteDataSource()

    @JvmStatic
    @Singleton
    @Provides
    fun provideNewsSettingsLocalDataSource(context: Context): NewsSettingsLocalDataSource =
        NewsSettingsLocalDataSource(context)

    @JvmStatic
    @Singleton
    @Provides
    fun provideNewsSettingsRepository(
        newsSettingsRemoteDataSource: NewsSettingsRemoteDataSource,
        newsSettingsLocalDataSource: NewsSettingsLocalDataSource
    ): NewsSettingsRepository =
        NewsSettingsRepository(newsSettingsRemoteDataSource, newsSettingsLocalDataSource)

    @JvmStatic
    @Singleton
    @Provides
    fun provideLoadNewsSettingsUseCase(newsSettingsRepository: NewsSettingsRepository): LoadNewsSettingsUseCase =
        LoadNewsSettingsUseCase(newsSettingsRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideLoadNewsLanguagesUseCase(newsSettingsRepository: NewsSettingsRepository): LoadNewsLanguagesUseCase =
        LoadNewsLanguagesUseCase(newsSettingsRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideSetUserPreferenceLanguageUseCase(newsSettingsRepository: NewsSettingsRepository): SetUserPreferenceLanguageUseCase =
        SetUserPreferenceLanguageUseCase(newsSettingsRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideSetUserPreferenceCategoriesUseCase(newsSettingsRepository: NewsSettingsRepository): SetUserPreferenceCategoriesUseCase =
        SetUserPreferenceCategoriesUseCase(newsSettingsRepository)

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
    fun provideNewsRepository(
        context: Context,
        configurations: HashMap<String, String>
    ): Repository<out NewsItem> {
        val url = String.format(
            Locale.getDefault(),
            configurations[NewsRepository.CONFIG_URL] ?: "",
            configurations[NewsRepository.CONFIG_CATEGORY],
            configurations[NewsRepository.CONFIG_LANGUAGE],
            "%d",
            "%d"
        )
        return RepositoryNewsPoint(context, url)
    }
}
