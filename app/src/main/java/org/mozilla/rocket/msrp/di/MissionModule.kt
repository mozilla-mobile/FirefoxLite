package org.mozilla.rocket.msrp.di

import dagger.Module
import dagger.Provides
import org.mozilla.rocket.msrp.data.MissionRepository
import org.mozilla.rocket.msrp.data.UserRepository
import org.mozilla.rocket.msrp.domain.LoadMissionsUseCase
import org.mozilla.rocket.msrp.domain.RedeemUseCase
import org.mozilla.rocket.msrp.ui.MissionViewModel
import javax.inject.Singleton

@Module
object MissionModule {

    @JvmStatic
    @Singleton
    @Provides
    fun provideMissionRepo(): MissionRepository = MissionRepository()

    @JvmStatic
    @Singleton
    @Provides
    fun provideUserRepo(): UserRepository = UserRepository()

    @JvmStatic
    @Singleton
    @Provides
    fun providLoadMissionsUseCase(missionRepository: MissionRepository): LoadMissionsUseCase =
        LoadMissionsUseCase(missionRepository)

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
        redeemUseCase: RedeemUseCase
    ): MissionViewModel = MissionViewModel(loadMissionsUseCase, redeemUseCase)
}