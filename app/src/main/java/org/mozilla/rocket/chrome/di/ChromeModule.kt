package org.mozilla.rocket.chrome.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.mozilla.rocket.chrome.BottomBarViewModelFactory
import org.mozilla.rocket.chrome.MenuViewModelFactory
import org.mozilla.rocket.chrome.PrivateBottomBarViewModelFactory
import org.mozilla.rocket.download.DownloadInfoRepository
import org.mozilla.rocket.download.DownloadViewModelFactory
import org.mozilla.rocket.urlinput.GlobalDataSource
import org.mozilla.rocket.urlinput.LocaleDataSource
import org.mozilla.rocket.urlinput.QuickSearchRepository
import org.mozilla.rocket.urlinput.QuickSearchViewModelFactory
import javax.inject.Singleton

@Module
object ChromeModule {

    @JvmStatic
    @Singleton
    @Provides
    fun provideGlobalDataSource(appContext: Context): GlobalDataSource =
            GlobalDataSource(appContext)

    @JvmStatic
    @Singleton
    @Provides
    fun provideLocaleDataSource(appContext: Context): LocaleDataSource =
            LocaleDataSource(appContext)

    @JvmStatic
    @Singleton
    @Provides
    fun provideQuickSearchRepository(
        globalDataSource: GlobalDataSource,
        localeDataSource: LocaleDataSource
    ): QuickSearchRepository = QuickSearchRepository(globalDataSource, localeDataSource)

    @JvmStatic
    @Singleton
    @Provides
    fun provideQuickSearchViewModelFactory(quickSearchRepository: QuickSearchRepository): QuickSearchViewModelFactory =
            QuickSearchViewModelFactory(quickSearchRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideDownloadInfoRepository(): DownloadInfoRepository = DownloadInfoRepository()

    @JvmStatic
    @Singleton
    @Provides
    fun provideDownloadViewModelFactory(downloadInfoRepository: DownloadInfoRepository): DownloadViewModelFactory =
            DownloadViewModelFactory(downloadInfoRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideMenuViewModelFactory(): MenuViewModelFactory = MenuViewModelFactory()

    @JvmStatic
    @Singleton
    @Provides
    fun provideBottomBarViewModelFactory(): BottomBarViewModelFactory = BottomBarViewModelFactory()

    @JvmStatic
    @Singleton
    @Provides
    fun providePrivateBottomBarViewModelFactory(): PrivateBottomBarViewModelFactory = PrivateBottomBarViewModelFactory()
}
