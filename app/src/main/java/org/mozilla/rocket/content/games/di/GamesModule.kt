package org.mozilla.rocket.content.games.di

import dagger.Module
import dagger.Provides
import org.mozilla.rocket.content.games.GamesViewModelFactory
import org.mozilla.rocket.content.games.data.GamesRepo
import javax.inject.Singleton

@Module
object GamesModule {

    @JvmStatic
    @Singleton
    @Provides
    fun provideGamesRepo(): GamesRepo = GamesRepo()

    @JvmStatic
    @Singleton
    @Provides
    fun provideGamesViewModelFactory(gamesRepo: GamesRepo): GamesViewModelFactory =
            GamesViewModelFactory(gamesRepo)
}