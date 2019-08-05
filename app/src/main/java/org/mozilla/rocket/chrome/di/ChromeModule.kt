package org.mozilla.rocket.chrome.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.mozilla.focus.persistence.BookmarksDatabase
import org.mozilla.focus.repository.BookmarkRepository
import org.mozilla.focus.utils.Browsers
import org.mozilla.focus.utils.Settings
import org.mozilla.rocket.chrome.BottomBarViewModelFactory
import org.mozilla.rocket.chrome.ChromeViewModelFactory
import org.mozilla.rocket.chrome.MenuViewModelFactory
import org.mozilla.rocket.chrome.PrivateBottomBarViewModelFactory
import org.mozilla.rocket.download.DownloadInfoRepository
import org.mozilla.rocket.download.DownloadViewModelFactory
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

    @JvmStatic
    @Singleton
    @Provides
    fun provideChromeViewModelFactory(appContext: Context): ChromeViewModelFactory {
        // TODO: use Dagger to provide these dependencies
        val settings = Settings.getInstance(appContext)
        val bookmarkRepo = BookmarkRepository.getInstance(BookmarksDatabase.getInstance(appContext))
        val privateMode = PrivateMode.getInstance(appContext)
        val browsers = Browsers(appContext, "http://mozilla.org")
        val storageHelper = StorageHelper(appContext)

        return ChromeViewModelFactory(settings, bookmarkRepo, privateMode, browsers, storageHelper)
    }
}
