package org.mozilla.rocket.content.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.mozilla.lite.newspoint.RepositoryNewsPoint
import org.mozilla.lite.partner.NewsItem
import org.mozilla.lite.partner.Repository
import org.mozilla.rocket.content.news.FakeNewsCategoryRepository
import org.mozilla.rocket.content.news.NewsViewModelFactory
import org.mozilla.rocket.content.news.data.NewsRepository
import java.util.Locale
import javax.inject.Singleton

@Module
class ContentModule {

    @Singleton
    @Provides
    fun provideNewsCategoryRepository(context: Context): FakeNewsCategoryRepository {
        return FakeNewsCategoryRepository(context)
    }

    @Singleton
    @Provides
    fun provideNewsViewModelFactory(newsCategoryRepository: FakeNewsCategoryRepository): NewsViewModelFactory {
        return NewsViewModelFactory(newsCategoryRepository)
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