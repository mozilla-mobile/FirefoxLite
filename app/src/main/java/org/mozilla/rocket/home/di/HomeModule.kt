package org.mozilla.rocket.home.di

import dagger.Module
import dagger.Provides
import org.mozilla.focus.utils.Settings
import org.mozilla.rocket.home.HomeViewModelFactory
import javax.inject.Singleton

@Module
object HomeModule {

    @JvmStatic
    @Singleton
    @Provides
    fun provideHomeViewModelFactory(settings: Settings): HomeViewModelFactory =
            HomeViewModelFactory(settings)
}