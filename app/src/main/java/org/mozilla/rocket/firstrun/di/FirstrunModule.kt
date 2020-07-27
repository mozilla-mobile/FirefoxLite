package org.mozilla.rocket.firstrun.di

import dagger.Module
import dagger.Provides
import org.mozilla.rocket.firstrun.FirstrunViewModel

@Module
object FirstrunModule {

    @JvmStatic
    @Provides
    fun provideFirstrunViewModel(): FirstrunViewModel = FirstrunViewModel()
}