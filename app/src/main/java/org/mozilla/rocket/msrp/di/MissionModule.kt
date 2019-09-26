package org.mozilla.rocket.msrp.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.mozilla.rocket.msrp.data.MissionLocalDataSource
import org.mozilla.rocket.msrp.data.MissionRemoteDataSource
import org.mozilla.rocket.msrp.data.MissionRepository
import org.mozilla.rocket.msrp.data.UserRepository
import org.mozilla.rocket.msrp.domain.HasUnreadMissionsUseCase
import org.mozilla.rocket.msrp.domain.LoadMissionsUseCase
import org.mozilla.rocket.msrp.domain.ReadMissionUseCase
import org.mozilla.rocket.msrp.domain.RedeemUseCase
import org.mozilla.rocket.msrp.ui.MissionViewModel
import javax.inject.Singleton

@Module
object MissionModule {

    @JvmStatic
    @Singleton
    @Provides
    fun provideUserRepo(): UserRepository = UserRepository()

    @JvmStatic
    @Singleton
    @Provides
    fun provideMissionLocalDataSource(appContext: Context): MissionLocalDataSource =
            MissionLocalDataSource(appContext)

    @JvmStatic
    @Singleton
    @Provides
    fun provideMissionRemoteDataSource(): MissionRemoteDataSource = MissionRemoteDataSource()

    @JvmStatic
    @Singleton
    @Provides
    fun provideMissionRepo(
        missionLocalDataSource: MissionLocalDataSource,
        missionRemoteDataSource: MissionRemoteDataSource
    ): MissionRepository = MissionRepository(
        missionLocalDataSource,
        missionRemoteDataSource
    )

    @JvmStatic
    @Singleton
    @Provides
    fun provideLoadMissionsUseCase(
        missionRepository: MissionRepository,
        userRepository: UserRepository
    ): LoadMissionsUseCase = LoadMissionsUseCase(missionRepository, userRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideReadMissionUseCase(missionRepository: MissionRepository) =
            ReadMissionUseCase(missionRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideHasUnreadMissionsUseCase() = HasUnreadMissionsUseCase()

    @JvmStatic
    @Singleton
    @Provides
    fun provideRedeemUseCase(
        missionRepository: MissionRepository,
        userRepository: UserRepository
    ) = RedeemUseCase(missionRepository, userRepository)

    @JvmStatic
    @Provides
    fun provideMissionViewModel(
        loadMissionsUseCase: LoadMissionsUseCase,
        readMissionUseCase: ReadMissionUseCase,
        hasUnreadMissionsUseCase: HasUnreadMissionsUseCase,
        redeemUseCase: RedeemUseCase
    ): MissionViewModel = MissionViewModel(
        loadMissionsUseCase,
        readMissionUseCase,
        hasUnreadMissionsUseCase,
        redeemUseCase
    )
}