package org.mozilla.rocket.content.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.mozilla.lite.newspoint.RepositoryNewsPoint
import org.mozilla.lite.partner.NewsItem
import org.mozilla.lite.partner.Repository
import org.mozilla.rocket.content.common.ui.ContentTabBottomBarViewModel
import org.mozilla.rocket.content.ecommerce.di.ShoppingModule
import org.mozilla.rocket.content.games.di.GamesModule
import org.mozilla.rocket.content.news.domain.LoadNewsSettingsUseCase
import org.mozilla.rocket.content.news.ui.NewsViewModel
import org.mozilla.rocket.content.news.data.NewsRepository
import org.mozilla.rocket.content.news.data.NewsSettingsLocalDataSource
import org.mozilla.rocket.content.news.data.NewsSettingsRemoteDataSource
import org.mozilla.rocket.content.news.data.NewsSettingsRepository
import java.util.Locale
import javax.inject.Singleton

@Module(includes = [GamesModule::class, ShoppingModule::class])
object ContentModule {

    @JvmStatic
    @Singleton
    @Provides
    fun provideNewsSettingsRemoteDataSource(): NewsSettingsRemoteDataSource {
        return NewsSettingsRemoteDataSource()
    }

    @JvmStatic
    @Singleton
    @Provides
    fun provideNewsSettingsLocalDataSource(context: Context): NewsSettingsLocalDataSource {
        return NewsSettingsLocalDataSource(context)
    }

    @JvmStatic
    @Singleton
    @Provides
    fun provideNewsSettingsRepository(
        newsSettingsRemoteDataSource: NewsSettingsRemoteDataSource,
        newsSettingsLocalDataSource: NewsSettingsLocalDataSource
    ): NewsSettingsRepository {
        return NewsSettingsRepository(newsSettingsRemoteDataSource, newsSettingsLocalDataSource)
    }

    @JvmStatic
    @Singleton
    @Provides
    fun provideLoadNewsSettingsUseCase(newsSettingsRepository: NewsSettingsRepository): LoadNewsSettingsUseCase =
        LoadNewsSettingsUseCase(newsSettingsRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideNewsViewModel(loadNewsSettingsUseCase: LoadNewsSettingsUseCase): NewsViewModel = NewsViewModel(loadNewsSettingsUseCase)

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

    @JvmStatic
    @Singleton
    @Provides
    fun provideContentTabBottomBarViewModel(): ContentTabBottomBarViewModel = ContentTabBottomBarViewModel()
}
