package org.mozilla.rocket.content.game.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.mozilla.rocket.content.game.data.GameLocalDataSource
import org.mozilla.rocket.content.game.data.GameRemoteDataSource
import org.mozilla.rocket.content.game.data.GameRepository
import org.mozilla.rocket.content.game.domain.AddRecentlyPlayedGameUseCase
import org.mozilla.rocket.content.game.domain.GetBitmapFromImageLinkUseCase
import org.mozilla.rocket.content.game.domain.GetDownloadGameListUseCase
import org.mozilla.rocket.content.game.domain.GetInstantGameListUseCase
import org.mozilla.rocket.content.game.domain.GetMyGameListUseCase
import org.mozilla.rocket.content.game.domain.GetRecentlyPlayedGameListUseCase
import org.mozilla.rocket.content.game.domain.RemoveRecentlyPlayedGameUseCase
import org.mozilla.rocket.content.game.domain.SetRecentPlayedSpotlightIsShownUseCase
import org.mozilla.rocket.content.game.domain.ShouldShowRecentPlayedSpotlightUseCase
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
    fun provideGameLocalDataSource(appContext: Context): GameLocalDataSource =
        GameLocalDataSource(appContext)

    @JvmStatic
    @Singleton
    @Provides
    fun provideGameRepository(gameRemoteDataSource: GameRemoteDataSource, gameLocalDataSource: GameLocalDataSource): GameRepository =
        GameRepository(gameRemoteDataSource, gameLocalDataSource)

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
    @Singleton
    @Provides
    fun provideAddRecentlyPlayedGameUseCase(repo: GameRepository): AddRecentlyPlayedGameUseCase =
        AddRecentlyPlayedGameUseCase(repo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideRemoveRecentlyPlayedGameUseCase(repo: GameRepository): RemoveRecentlyPlayedGameUseCase =
        RemoveRecentlyPlayedGameUseCase(repo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideGetRecentlyPlayedGameListUseCase(repo: GameRepository): GetRecentlyPlayedGameListUseCase =
        GetRecentlyPlayedGameListUseCase(repo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideBitmapFromImageLinkUseCase(repo: GameRepository): GetBitmapFromImageLinkUseCase =
        GetBitmapFromImageLinkUseCase(repo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideGetMyGameListUseCase(repo: GameRepository): GetMyGameListUseCase =
        GetMyGameListUseCase(repo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideShouldShowRecentPlayedOnboardingUseCase(repo: GameRepository): ShouldShowRecentPlayedSpotlightUseCase =
            ShouldShowRecentPlayedSpotlightUseCase(repo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideSetRecentPlayedOnboardingIsShownUseCase(repo: GameRepository): SetRecentPlayedSpotlightIsShownUseCase =
            SetRecentPlayedSpotlightIsShownUseCase(repo)

    @JvmStatic
    @Provides
    fun provideInstantGameViewModel(
        getInstantGameListUseCase: GetInstantGameListUseCase,
        addRecentlyPlayedGame: AddRecentlyPlayedGameUseCase,
        removeRecentlyPlayedGame: RemoveRecentlyPlayedGameUseCase,
        getRecentlyPlayedGameList: GetRecentlyPlayedGameListUseCase,
        getBitmapFromImageLinkUseCase: GetBitmapFromImageLinkUseCase,
        shouldShowRecentPlayedSpotlightUseCase: ShouldShowRecentPlayedSpotlightUseCase,
        setRecentPlayedSpotlightIsShownUseCase: SetRecentPlayedSpotlightIsShownUseCase
    ): InstantGameViewModel =
        InstantGameViewModel(
            getInstantGameListUseCase,
            addRecentlyPlayedGame,
            removeRecentlyPlayedGame,
            getRecentlyPlayedGameList,
            getBitmapFromImageLinkUseCase,
            shouldShowRecentPlayedSpotlightUseCase,
            setRecentPlayedSpotlightIsShownUseCase
        )

    @JvmStatic
    @Provides
    fun provideDownloadGameViewModel(
        getDownloadGameListUseCase: GetDownloadGameListUseCase,
        getMyGameListUseCase: GetMyGameListUseCase
    ): DownloadGameViewModel =
        DownloadGameViewModel(getDownloadGameListUseCase, getMyGameListUseCase)
}