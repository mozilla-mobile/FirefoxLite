package org.mozilla.rocket.msrp.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.mozilla.focus.utils.FirebaseHelper
import org.mozilla.rocket.msrp.data.MissionLocalDataSource
import org.mozilla.rocket.msrp.data.MissionRemoteDataSource
import org.mozilla.rocket.msrp.data.MissionRepository
import org.mozilla.rocket.msrp.data.UserRepository
import org.mozilla.rocket.msrp.domain.BindFxAccountUseCase
import org.mozilla.rocket.msrp.domain.CheckInMissionUseCase
import org.mozilla.rocket.msrp.domain.CompleteJoinMissionOnboardingUseCase
import org.mozilla.rocket.msrp.domain.GetApkDownloadLinkUseCase
import org.mozilla.rocket.msrp.domain.GetChallengeMissionsUseCase
import org.mozilla.rocket.msrp.domain.GetContentHubClickOnboardingEventUseCase
import org.mozilla.rocket.msrp.domain.GetCouponUseCase
import org.mozilla.rocket.msrp.domain.GetIsFxAccountUseCase
import org.mozilla.rocket.msrp.domain.GetRedeemMissionsUseCase
import org.mozilla.rocket.msrp.domain.GetUserIdUseCase
import org.mozilla.rocket.msrp.domain.HasUnreadMissionsUseCase
import org.mozilla.rocket.msrp.domain.IsFxAccountUseCase
import org.mozilla.rocket.msrp.domain.IsNeedJoinMissionOnboardingUseCase
import org.mozilla.rocket.msrp.domain.JoinMissionUseCase
import org.mozilla.rocket.msrp.domain.QuitMissionUseCase
import org.mozilla.rocket.msrp.domain.ReadMissionUseCase
import org.mozilla.rocket.msrp.domain.RedeemUseCase
import org.mozilla.rocket.msrp.domain.RefreshMissionsUseCase
import org.mozilla.rocket.msrp.domain.RequestContentHubClickOnboardingUseCase
import org.mozilla.rocket.msrp.ui.MissionCouponViewModel
import org.mozilla.rocket.msrp.ui.MissionDetailViewModel
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
        appContext: Context,
        missionLocalDataSource: MissionLocalDataSource,
        missionRemoteDataSource: MissionRemoteDataSource
    ): MissionRepository = MissionRepository(
        appContext,
        missionLocalDataSource,
        missionRemoteDataSource
    )

    @JvmStatic
    @Singleton
    @Provides
    fun provideGetChallengeMissionsUseCase(
        missionRepository: MissionRepository
    ): GetChallengeMissionsUseCase = GetChallengeMissionsUseCase(missionRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideGetMissionsUseCase(
        missionRepository: MissionRepository
    ): GetRedeemMissionsUseCase = GetRedeemMissionsUseCase(missionRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideRefreshMissionsUseCase(
        missionRepository: MissionRepository,
        userRepository: UserRepository
    ): RefreshMissionsUseCase = RefreshMissionsUseCase(missionRepository, userRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideReadMissionUseCase(missionRepository: MissionRepository) =
            ReadMissionUseCase(missionRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideHasUnreadMissionsUseCase(missionRepository: MissionRepository) =
            HasUnreadMissionsUseCase(missionRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideJoinMissionUseCase(
        missionRepository: MissionRepository,
        userRepository: UserRepository
    ) = JoinMissionUseCase(missionRepository, userRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideQuitMissionUseCase(
        missionRepository: MissionRepository,
        userRepository: UserRepository
    ) = QuitMissionUseCase(missionRepository, userRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideCheckInMissionUseCase(
        missionRepository: MissionRepository,
        userRepository: UserRepository
    ) = CheckInMissionUseCase(missionRepository, userRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideRedeemUseCase(
        missionRepository: MissionRepository,
        userRepository: UserRepository
    ) = RedeemUseCase(missionRepository, userRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideIsFxAccountUseCase(
        userRepository: UserRepository
    ) = IsFxAccountUseCase(userRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideGetIsFxAccountUseCase(
        userRepository: UserRepository
    ) = GetIsFxAccountUseCase(userRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideGetUserIdUseCase(
        userRepository: UserRepository
    ) = GetUserIdUseCase(userRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideGetCouponUseCase(
        missionRepository: MissionRepository,
        userRepository: UserRepository
    ) = GetCouponUseCase(missionRepository, userRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideBindFxAccountUseCase(
        userRepository: UserRepository
    ) = BindFxAccountUseCase(userRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideIsNeedJoinMissionOnboardingUseCase(
        missionRepository: MissionRepository
    ) = IsNeedJoinMissionOnboardingUseCase(missionRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideCompleteJoinMissionOnboardingUseCase(
        missionRepository: MissionRepository
    ) = CompleteJoinMissionOnboardingUseCase(missionRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideGetContentHubClickOnboardingEventUseCase(
        missionRepository: MissionRepository
    ) = GetContentHubClickOnboardingEventUseCase(missionRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideRequestContentHubClickOnboardingUseCase(
        missionRepository: MissionRepository
    ) = RequestContentHubClickOnboardingUseCase(missionRepository)

    @JvmStatic
    @Provides
    fun provideMissionViewModel(
        getChallengeMissionsUseCase: GetChallengeMissionsUseCase,
        getRedeemMissionsUseCase: GetRedeemMissionsUseCase,
        refreshMissionsUseCase: RefreshMissionsUseCase
    ): MissionViewModel = MissionViewModel(
        getChallengeMissionsUseCase,
        getRedeemMissionsUseCase,
        refreshMissionsUseCase
    )

    @JvmStatic
    @Provides
    fun provideMissionDetailViewModel(
        readMissionUseCase: ReadMissionUseCase,
        joinMissionUseCase: JoinMissionUseCase,
        quitMissionUseCase: QuitMissionUseCase,
        refreshMissionsUseCase: RefreshMissionsUseCase,
        redeemUseCase: RedeemUseCase,
        isFxAccountUseCase: IsFxAccountUseCase,
        getUserIdUseCase: GetUserIdUseCase,
        bindFxAccountUseCase: BindFxAccountUseCase,
        isNeedJoinMissionOnboardingUseCase: IsNeedJoinMissionOnboardingUseCase,
        requestContentHubClickOnboardingUseCase: RequestContentHubClickOnboardingUseCase,
        getIsFxAccountUseCase: GetIsFxAccountUseCase,
        getApkDownloadLinkUseCase: GetApkDownloadLinkUseCase
    ): MissionDetailViewModel = MissionDetailViewModel(
        readMissionUseCase,
        joinMissionUseCase,
        quitMissionUseCase,
        refreshMissionsUseCase,
        redeemUseCase,
        isFxAccountUseCase,
        getUserIdUseCase,
        bindFxAccountUseCase,
        isNeedJoinMissionOnboardingUseCase,
        requestContentHubClickOnboardingUseCase,
        getIsFxAccountUseCase,
        getApkDownloadLinkUseCase
    )

    @JvmStatic
    @Provides
    fun provideMissionCouponViewModel(
        getCouponUseCase: GetCouponUseCase
    ): MissionCouponViewModel = MissionCouponViewModel(
        getCouponUseCase
    )

    @JvmStatic
    @Provides
    fun provideGetApkDownloadLinkUseCase(): GetApkDownloadLinkUseCase =
            GetApkDownloadLinkUseCase(FirebaseHelper.getFirebase())
}