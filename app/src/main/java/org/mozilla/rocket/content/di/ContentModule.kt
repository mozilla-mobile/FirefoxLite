package org.mozilla.rocket.content.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.mozilla.lite.newspoint.RepositoryNewsPoint
import org.mozilla.lite.partner.NewsItem
import org.mozilla.lite.partner.Repository
import org.mozilla.rocket.content.news.NewsViewModelFactory
import org.mozilla.rocket.content.news.data.NewsRepository
import org.mozilla.rocket.content.news.data.NewsSettingsLocalDataSource
import org.mozilla.rocket.content.news.data.NewsSettingsRemoteDataSource
import org.mozilla.rocket.content.news.data.NewsSettingsRepository
import java.util.Locale
import javax.inject.Singleton

@Module
class ContentModule {

    @Singleton
    @Provides
    fun provideNewsSettingsRemoteDataSource(): NewsSettingsRemoteDataSource {
        return NewsSettingsRemoteDataSource()
    }

    @Singleton
    @Provides
    fun provideNewsSettingsLocalDataSource(context: Context): NewsSettingsLocalDataSource {
        return NewsSettingsLocalDataSource(context)
    }

    @Singleton
    @Provides
    fun provideNewsSettingsRepository(
        newsSettingsRemoteDataSource: NewsSettingsRemoteDataSource,
        newsSettingsLocalDataSource: NewsSettingsLocalDataSource
    ): NewsSettingsRepository {
        return NewsSettingsRepository(newsSettingsRemoteDataSource, newsSettingsLocalDataSource)
    }

    @Singleton
    @Provides
    fun provideNewsViewModelFactory(newsSettingsRepository: NewsSettingsRepository): NewsViewModelFactory {
        return NewsViewModelFactory(newsSettingsRepository)
    }

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
