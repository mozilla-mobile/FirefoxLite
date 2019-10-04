package org.mozilla.rocket.content.games.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.mozilla.rocket.content.games.data.GameLocalDataSource
import org.mozilla.rocket.content.games.data.GameRepository
import org.mozilla.rocket.content.games.ui.GamesViewModel
import javax.inject.Singleton

@Module
object GamesModule {

    @JvmStatic
    @Singleton
    @Provides
    fun provideGameLocalDataSource(appContext: Context): GameLocalDataSource =
        GameLocalDataSource(appContext)

    @JvmStatic
    @Singleton
    @Provides
    fun provideGameRepository(gameDataSource: GameLocalDataSource): GameRepository =
        GameRepository(gameDataSource)

    @JvmStatic
    @Provides
    fun provideGamesViewModel(gameRepository: GameRepository): GamesViewModel =
        GamesViewModel(gameRepository)
}