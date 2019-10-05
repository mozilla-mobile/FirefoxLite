package org.mozilla.rocket.content.game.di

import dagger.Module
import dagger.Provides
import org.mozilla.rocket.content.game.data.GameRemoteDataSource
import org.mozilla.rocket.content.game.data.GameRepository
import org.mozilla.rocket.content.game.domain.GetDownloadGameListUseCase
import org.mozilla.rocket.content.game.domain.GetInstantGameListUseCase
import org.mozilla.rocket.content.game.ui.DownloadGameViewModel
import org.mozilla.rocket.content.game.ui.InstantGameViewModel
import javax.inject.Singleton

@Module
object GameModule {

    @JvmStatic
    @Singleton
    @Provides
    fun provideGameRemoteDataSource(): GameRemoteDataSource =
        GameRemoteDataSource()

    @JvmStatic
    @Singleton
    @Provides
    fun provideGameRepository(gameRemoteDataSource: GameRemoteDataSource): GameRepository =
        GameRepository(gameRemoteDataSource)

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
    fun provideInstantGameViewModel(getInstantGameListUseCase: GetInstantGameListUseCase): InstantGameViewModel =
        InstantGameViewModel(getInstantGameListUseCase)

    @JvmStatic
    @Provides
    fun provideDownloadGameViewModel(getDownloadGameListUseCase: GetDownloadGameListUseCase): DownloadGameViewModel =
        DownloadGameViewModel(getDownloadGameListUseCase)
}