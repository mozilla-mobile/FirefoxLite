package org.mozilla.rocket.chrome.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.mozilla.focus.persistence.BookmarksDatabase
import org.mozilla.focus.repository.BookmarkRepository
import org.mozilla.focus.utils.Browsers
import org.mozilla.focus.utils.NewFeatureNotice
import org.mozilla.focus.utils.Settings
import org.mozilla.focus.viewmodel.BookmarkViewModel
import org.mozilla.focus.viewmodel.ShoppingSearchPromptViewModel
import org.mozilla.rocket.chrome.BottomBarViewModel
import org.mozilla.rocket.chrome.ChromeViewModel
import org.mozilla.rocket.chrome.MenuViewModel
import org.mozilla.rocket.chrome.PrivateBottomBarViewModel
import org.mozilla.rocket.download.DownloadIndicatorViewModel
import org.mozilla.rocket.download.DownloadInfoRepository
import org.mozilla.rocket.download.DownloadInfoViewModel
import org.mozilla.rocket.helper.StorageHelper
import org.mozilla.rocket.privately.PrivateMode
import org.mozilla.rocket.settings.defaultbrowser.data.DefaultBrowserLocalDataSource
import org.mozilla.rocket.settings.defaultbrowser.data.DefaultBrowserRepository
import org.mozilla.rocket.settings.defaultbrowser.ui.DefaultBrowserPreferenceViewModel
import org.mozilla.rocket.shopping.search.domain.GetSearchPromptMessageShowCountUseCase
import org.mozilla.rocket.shopping.search.domain.GetShoppingSitesUseCase
import org.mozilla.rocket.shopping.search.domain.SetSearchPromptMessageShowCountUseCase
import org.mozilla.rocket.urlinput.GlobalDataSource
import org.mozilla.rocket.urlinput.LocaleDataSource
import org.mozilla.rocket.urlinput.QuickSearchRepository
import org.mozilla.rocket.urlinput.QuickSearchViewModel
import javax.inject.Singleton

@Module
object ChromeModule {

    @JvmStatic
    @Singleton
    @Provides
    fun provideGlobalDataSource(appContext: Context): GlobalDataSource = GlobalDataSource(appContext)

    @JvmStatic
    @Singleton
    @Provides
    fun provideLocaleDataSource(appContext: Context): LocaleDataSource = LocaleDataSource(appContext)

    @JvmStatic
    @Singleton
    @Provides
    fun provideQuickSearchRepository(
        globalDataSource: GlobalDataSource,
        localeDataSource: LocaleDataSource
    ): QuickSearchRepository = QuickSearchRepository(globalDataSource, localeDataSource)

    @JvmStatic
    @Provides
    fun provideQuickSearchViewModel(quickSearchRepository: QuickSearchRepository): QuickSearchViewModel = QuickSearchViewModel(quickSearchRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideDownloadInfoRepository(): DownloadInfoRepository = DownloadInfoRepository()

    @JvmStatic
    @Provides
    fun provideDownloadIndicatorViewModel(downloadInfoRepository: DownloadInfoRepository): DownloadIndicatorViewModel = DownloadIndicatorViewModel(downloadInfoRepository)

    @JvmStatic
    @Provides
    fun provideDownloadInfoViewModel(downloadInfoRepository: DownloadInfoRepository): DownloadInfoViewModel = DownloadInfoViewModel(downloadInfoRepository)

    @JvmStatic
    @Provides
    fun provideMenuViewModel(): MenuViewModel = MenuViewModel()

    @JvmStatic
    @Provides
    fun provideBottomBarViewModel(): BottomBarViewModel = BottomBarViewModel()

    @JvmStatic
    @Provides
    fun providePrivateBottomBarViewModel(): PrivateBottomBarViewModel = PrivateBottomBarViewModel()

    @JvmStatic
    @Singleton
    @Provides
    fun provideBookmarkRepository(appContext: Context): BookmarkRepository = BookmarkRepository.getInstance(BookmarksDatabase.getInstance(appContext))

    @JvmStatic
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
    @Provides
    fun provideChromeViewModel(
        settings: Settings,
        newFeatureNotice: NewFeatureNotice,
        bookmarkRepo: BookmarkRepository,
        privateMode: PrivateMode,
        browsers: Browsers,
        storageHelper: StorageHelper
    ): ChromeViewModel = ChromeViewModel(
        settings,
        newFeatureNotice,
        bookmarkRepo,
        privateMode,
        browsers,
        storageHelper
    )

    @JvmStatic
    @Provides
    fun provideShoppingSearchPromptViewModel(
        getShoppingSitesUseCase: GetShoppingSitesUseCase,
        getSearchPromptMessageShowCountUseCase: GetSearchPromptMessageShowCountUseCase,
        setSearchPromptMessageShowCountUseCase: SetSearchPromptMessageShowCountUseCase
    ): ShoppingSearchPromptViewModel =
        ShoppingSearchPromptViewModel(
            getShoppingSitesUseCase,
            getSearchPromptMessageShowCountUseCase,
            setSearchPromptMessageShowCountUseCase
        )

    @JvmStatic
    @Singleton
    @Provides
    fun provideDefaultBrowserLocalDataSource(appContext: Context): DefaultBrowserLocalDataSource =
        DefaultBrowserLocalDataSource(appContext)

    @JvmStatic
    @Singleton
    @Provides
    fun provideDefaultBrowserRepository(defaultBrowserLocalDataSource: DefaultBrowserLocalDataSource): DefaultBrowserRepository =
        DefaultBrowserRepository(defaultBrowserLocalDataSource)

    @JvmStatic
    @Provides
    fun provideDefaultBrowserPreferenceViewModel(defaultBrowserRepository: DefaultBrowserRepository): DefaultBrowserPreferenceViewModel =
        DefaultBrowserPreferenceViewModel(defaultBrowserRepository)
}
