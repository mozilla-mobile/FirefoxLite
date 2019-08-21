package org.mozilla.rocket.chrome.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.mozilla.focus.persistence.BookmarksDatabase
import org.mozilla.focus.repository.BookmarkRepository
import org.mozilla.focus.utils.Browsers
import org.mozilla.focus.utils.Settings
import org.mozilla.focus.viewmodel.BookmarkViewModel
import org.mozilla.rocket.chrome.BottomBarViewModel
import org.mozilla.rocket.chrome.ChromeViewModel
import org.mozilla.rocket.chrome.MenuViewModel
import org.mozilla.rocket.chrome.PrivateBottomBarViewModel
import org.mozilla.rocket.download.DownloadIndicatorViewModel
import org.mozilla.rocket.download.DownloadInfoRepository
import org.mozilla.rocket.download.DownloadInfoViewModel
import org.mozilla.rocket.helper.StorageHelper
import org.mozilla.rocket.privately.PrivateMode
import org.mozilla.rocket.urlinput.*
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
    fun provideQuickSearchViewModel(quickSearchRepository: QuickSearchRepository): QuickSearchViewModel = QuickSearchViewModel(quickSearchRepository)

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
    fun provideMenuViewModel(): MenuViewModel = MenuViewModel()

    @JvmStatic
    @Singleton
    @Provides
    fun provideBottomBarViewModel(): BottomBarViewModel = BottomBarViewModel()

    @JvmStatic
    @Singleton
    @Provides
    fun providePrivateBottomBarViewModel(): PrivateBottomBarViewModel = PrivateBottomBarViewModel()

    @JvmStatic
    @Singleton
    @Provides
    fun provideBookmarkRepository(appContext: Context): BookmarkRepository = BookmarkRepository.getInstance(BookmarksDatabase.getInstance(appContext))

    @JvmStatic
    @Singleton
    @Provides
    fun provideBookmarkViewModel(bookmarkRepository: BookmarkRepository): BookmarkViewModel = BookmarkViewModel(bookmarkRepository)

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
