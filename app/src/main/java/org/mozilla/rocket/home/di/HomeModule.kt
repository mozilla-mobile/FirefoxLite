package org.mozilla.rocket.home.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.mozilla.focus.utils.Settings
import org.mozilla.rocket.home.HomeViewModelFactory
import org.mozilla.rocket.home.contenthub.data.ContentHubRepo
import org.mozilla.rocket.home.contenthub.domain.GetContentHubItemsUseCase
import org.mozilla.rocket.home.topsites.data.PinSiteManager
import org.mozilla.rocket.home.topsites.data.SharedPreferencePinSiteDelegate
import org.mozilla.rocket.home.topsites.data.TopSitesRepo
import org.mozilla.rocket.home.topsites.domain.GetTopSitesUseCase
import org.mozilla.rocket.home.topsites.domain.PinTopSiteUseCase
import org.mozilla.rocket.home.topsites.domain.RemoveTopSiteUseCase
import org.mozilla.rocket.home.topsites.domain.TopSitesConfigsUseCase
import javax.inject.Singleton

@Module
object HomeModule {

    @JvmStatic
    @Singleton
    @Provides
    fun provideHomeViewModelFactory(
        settings: Settings,
        getTopSitesUseCase: GetTopSitesUseCase,
        topSitesConfigsUseCase: TopSitesConfigsUseCase,
        pinTopSiteUseCase: PinTopSiteUseCase,
        removeTopSiteUseCase: RemoveTopSiteUseCase,
        getContentHubItemsUseCase: GetContentHubItemsUseCase
    ): HomeViewModelFactory = HomeViewModelFactory(
        settings,
        getTopSitesUseCase,
        topSitesConfigsUseCase,
        pinTopSiteUseCase,
        removeTopSiteUseCase,
        getContentHubItemsUseCase
    )

    @JvmStatic
    @Singleton
    @Provides
    fun provideGetTopSitesUseCase(topSitesRepo: TopSitesRepo): GetTopSitesUseCase = GetTopSitesUseCase(topSitesRepo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideTopSitesConfigsUseCase(topSitesRepo: TopSitesRepo): TopSitesConfigsUseCase = TopSitesConfigsUseCase(topSitesRepo)

    @JvmStatic
    @Singleton
    @Provides
    fun providePinTopSiteUseCase(topSitesRepo: TopSitesRepo): PinTopSiteUseCase = PinTopSiteUseCase(topSitesRepo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideRemoveTopSiteUseCase(topSitesRepo: TopSitesRepo): RemoveTopSiteUseCase = RemoveTopSiteUseCase(topSitesRepo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideTopSitesRepo(
        appContext: Context,
        pinSiteManager: PinSiteManager
    ): TopSitesRepo = TopSitesRepo(appContext, pinSiteManager)

    @JvmStatic
    @Singleton
    @Provides
    fun providePinSiteManager(appContext: Context): PinSiteManager =
            PinSiteManager(SharedPreferencePinSiteDelegate(appContext))

    @JvmStatic
    @Singleton
    @Provides
    fun provideGetContentHubItemsUseCase(contentHubRepo: ContentHubRepo): GetContentHubItemsUseCase = GetContentHubItemsUseCase(contentHubRepo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideContentHubRepo(appContext: Context): ContentHubRepo = ContentHubRepo(appContext)
}