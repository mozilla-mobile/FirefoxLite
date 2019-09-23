package org.mozilla.rocket.content.games.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.mozilla.rocket.content.games.data.GamesRepo
import org.mozilla.rocket.content.games.ui.GamesViewModel
import javax.inject.Singleton

@Module
object GamesModule {

    @JvmStatic
    @Singleton
    @Provides
    fun provideGamesRepo(appContext: Context): GamesRepo = GamesRepo(appContext)

    @JvmStatic
    @Provides
    fun provideGamesViewModel(gamesRepo: GamesRepo): GamesViewModel = GamesViewModel(gamesRepo)
}