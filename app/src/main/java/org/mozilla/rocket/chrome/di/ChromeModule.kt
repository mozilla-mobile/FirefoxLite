package org.mozilla.rocket.chrome.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.mozilla.focus.persistence.BookmarksDatabase
import org.mozilla.focus.repository.BookmarkRepository
import org.mozilla.focus.utils.Browsers
import org.mozilla.focus.utils.Settings
import org.mozilla.rocket.chrome.*
import org.mozilla.rocket.download.DownloadIndicatorViewModel
import org.mozilla.rocket.download.DownloadInfoRepository
import org.mozilla.rocket.download.DownloadInfoViewModel
import org.mozilla.rocket.helper.StorageHelper
import org.mozilla.rocket.privately.PrivateMode
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
    fun provideDownloadIndicatorViewModel(downloadInfoRepository: DownloadInfoRepository): DownloadIndicatorViewModel = DownloadIndicatorViewModel(downloadInfoRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideDownloadInfoViewModel(downloadInfoRepository: DownloadInfoRepository): DownloadInfoViewModel = DownloadInfoViewModel(downloadInfoRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideMenuViewModelFactory(): MenuViewModelFactory = MenuViewModelFactory()

    @JvmStatic
    @Singleton
    @Provides
    fun provideBottomBarViewModel(): BottomBarViewModel = BottomBarViewModel()

    @JvmStatic
    @Singleton
    @Provides
    fun providePrivateBottomBarViewModelFactory(): PrivateBottomBarViewModelFactory = PrivateBottomBarViewModelFactory()

    @JvmStatic
    @Singleton
    @Provides
    fun provideBookmarkRepository(appContext: Context): BookmarkRepository = BookmarkRepository.getInstance(BookmarksDatabase.getInstance(appContext))

    @JvmStatic
    @Singleton
    @Provides
    fun providePrivateMode(appContext: Context): PrivateMode = PrivateMode.getInstance(appContext)

    @JvmStatic
    @Singleton
    @Provides
    fun provideBrowsers(appContext: Context): Browsers = Browsers(appContext, "http://mozilla.org")

    @JvmStatic
    @Singleton
    @Provides
    fun provideStorageHelper(appContext: Context): StorageHelper = StorageHelper(appContext)

    @JvmStatic
    @Singleton
    @Provides
    fun provideChromeViewModel(settings: Settings,
                               bookmarkRepo: BookmarkRepository,
                               privateMode: PrivateMode,
                               browsers: Browsers,
                               storageHelper: StorageHelper): ChromeViewModel =
            ChromeViewModel(settings, bookmarkRepo, privateMode, browsers, storageHelper)
}
