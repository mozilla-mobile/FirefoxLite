package org.mozilla.focus.home.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.mozilla.focus.home.repository.TopSitesRepo
import javax.inject.Singleton

@Module
object TopSitesModule {

    @JvmStatic
    @Singleton
    @Provides
    fun provideTopSitesRepo(appContext: Context): TopSitesRepo = TopSitesRepo(appContext)
}