package org.mozilla.rocket.content.games.di

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
    fun provideGamesRepo(): GamesRepo = GamesRepo()

    @JvmStatic
    @Provides
    fun provideGamesViewModel(gamesRepo: GamesRepo): GamesViewModel = GamesViewModel(gamesRepo)
}