package org.mozilla.rocket.content.games.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.mozilla.rocket.content.games.data.GameLocalDataSource
import org.mozilla.rocket.content.games.data.GameRepository
import org.mozilla.rocket.content.games.domain.GetDownloadGameListUseCase
import org.mozilla.rocket.content.games.domain.GetInstantGameListUseCase
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
    @Singleton
    @Provides
    fun provideGetInstantGameListUseCase(repo: GameRepository): GetInstantGameListUseCase =
        GetInstantGameListUseCase(repo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideGetDownloadGameListUseCase(repo: GameRepository): GetDownloadGameListUseCase =
        GetDownloadGameListUseCase(repo)

    @JvmStatic
    @Provides
    fun provideGamesViewModel(
        getInstantGameListUseCase: GetInstantGameListUseCase,
        getDownloadGameListUseCase: GetDownloadGameListUseCase
    ): GamesViewModel =
        GamesViewModel(getInstantGameListUseCase, getDownloadGameListUseCase)
}