package org.mozilla.rocket.chrome.di

import dagger.Module
import dagger.Provides
import org.mozilla.rocket.download.DownloadInfoRepository
import org.mozilla.rocket.download.DownloadViewModelFactory
import javax.inject.Singleton

@Module
object ChromeModule {

    @JvmStatic
    @Singleton
    @Provides
    fun provideDownloadInfoRepository(): DownloadInfoRepository = DownloadInfoRepository()

    @JvmStatic
    @Singleton
    @Provides
    fun provideDownloadViewModelFactory(downloadInfoRepository: DownloadInfoRepository): DownloadViewModelFactory =
            DownloadViewModelFactory(downloadInfoRepository)
}
