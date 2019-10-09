package org.mozilla.rocket.home.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.mockito.Mockito.spy
import org.mozilla.focus.utils.Settings
import org.mozilla.rocket.home.HomeViewModel
import org.mozilla.rocket.home.contenthub.data.ContentHubRepo
import org.mozilla.rocket.home.contenthub.domain.GetContentHubItemsUseCase
import org.mozilla.rocket.home.domain.IsShoppingButtonEnabledUseCase
import org.mozilla.rocket.home.logoman.data.LogoManNotificationRepo
import org.mozilla.rocket.home.logoman.domain.DismissLogoManNotificationUseCase
import org.mozilla.rocket.home.logoman.domain.GetLogoManNotificationUseCase
import org.mozilla.rocket.msrp.domain.IsMsrpAvailableUseCase
import org.mozilla.rocket.home.onboarding.CheckFirstRunUseCase
import org.mozilla.rocket.home.onboarding.CheckLiteUpdate
import org.mozilla.rocket.home.onboarding.CompleteFirstRunUseCase
import org.mozilla.rocket.home.onboarding.CompleteLiteUpdate
import org.mozilla.rocket.home.topsites.data.PinSiteManager
import org.mozilla.rocket.home.topsites.data.SharedPreferencePinSiteDelegate
import org.mozilla.rocket.home.topsites.data.TopSitesRepo
import org.mozilla.rocket.home.topsites.domain.GetTopSitesUseCase
import org.mozilla.rocket.home.topsites.domain.PinTopSiteUseCase
import org.mozilla.rocket.home.topsites.domain.RemoveTopSiteUseCase
import org.mozilla.rocket.home.topsites.domain.TopSitesConfigsUseCase
import org.mozilla.rocket.msrp.data.MissionRepository
import org.mozilla.rocket.msrp.domain.CheckInMissionUseCase
import org.mozilla.rocket.msrp.domain.CompleteJoinMissionOnboardingUseCase
import org.mozilla.rocket.msrp.domain.GetContentHubClickOnboardingEventUseCase
import org.mozilla.rocket.msrp.domain.GetIsFxAccountUseCase
import org.mozilla.rocket.msrp.domain.HasUnreadMissionsUseCase
import org.mozilla.rocket.msrp.domain.RefreshMissionsUseCase
import org.mozilla.rocket.shopping.search.data.ShoppingSearchRepository
import javax.inject.Singleton

@Module
object HomeModule {

    @JvmStatic
    @Provides
    fun provideHomeViewModel(
        settings: Settings,
        getTopSitesUseCase: GetTopSitesUseCase,
        topSitesConfigsUseCase: TopSitesConfigsUseCase,
        pinTopSiteUseCase: PinTopSiteUseCase,
        removeTopSiteUseCase: RemoveTopSiteUseCase,
        getContentHubItemsUseCase: GetContentHubItemsUseCase,
        getLogoManNotificationUseCase: GetLogoManNotificationUseCase,
        dismissLogoManNotificationUseCase: DismissLogoManNotificationUseCase,
        isMsrpAvailableUseCase: IsMsrpAvailableUseCase,
        isShoppingButtonEnabledUseCase: IsShoppingButtonEnabledUseCase,
        checkFirstRunUseCase: CheckFirstRunUseCase,
        completeFirstRunUseCase: CompleteFirstRunUseCase,
        checkLiteUpdate: CheckLiteUpdate,
        completeLiteUpdate: CompleteLiteUpdate,
        checkInMissionUseCase: CheckInMissionUseCase,
        completeJoinMissionOnboardingUseCase: CompleteJoinMissionOnboardingUseCase,
        getContentHubClickOnboardingEventUseCase: GetContentHubClickOnboardingEventUseCase,
        refreshMissionsUseCase: RefreshMissionsUseCase,
        hasUnreadMissionsUseCase: HasUnreadMissionsUseCase,
        getIsFxAccountUseCase: GetIsFxAccountUseCase
    ): HomeViewModel = HomeViewModel(
        settings,
        getTopSitesUseCase,
        topSitesConfigsUseCase,
        pinTopSiteUseCase,
        removeTopSiteUseCase,
        getContentHubItemsUseCase,
        getLogoManNotificationUseCase,
        dismissLogoManNotificationUseCase,
        isMsrpAvailableUseCase,
        isShoppingButtonEnabledUseCase,
        checkFirstRunUseCase,
        completeFirstRunUseCase,
        checkLiteUpdate,
        completeLiteUpdate,
        checkInMissionUseCase,
        completeJoinMissionOnboardingUseCase,
        getContentHubClickOnboardingEventUseCase,
        refreshMissionsUseCase,
        hasUnreadMissionsUseCase,
        getIsFxAccountUseCase
    )

    @JvmStatic
    @Singleton
    @Provides
    fun provideGetTopSitesUseCase(topSitesRepo: TopSitesRepo): GetTopSitesUseCase = spy(GetTopSitesUseCase(topSitesRepo))

    @JvmStatic
    @Singleton
    @Provides
    fun provideTopSitesConfigsUseCase(topSitesRepo: TopSitesRepo): TopSitesConfigsUseCase = TopSitesConfigsUseCase(topSitesRepo)

    @JvmStatic
    @Singleton
    @Provides
    fun providePinTopSiteUseCase(topSitesRepo: TopSitesRepo): PinTopSiteUseCase = PinTopSiteUseCase(topSitesRepo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideRemoveTopSiteUseCase(topSitesRepo: TopSitesRepo): RemoveTopSiteUseCase = RemoveTopSiteUseCase(topSitesRepo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideTopSitesRepo(
        appContext: Context,
        pinSiteManager: PinSiteManager
    ): TopSitesRepo = TopSitesRepo(appContext, pinSiteManager)

    @JvmStatic
    @Singleton
    @Provides
    fun providePinSiteManager(appContext: Context): PinSiteManager =
            PinSiteManager(SharedPreferencePinSiteDelegate(appContext))

    @JvmStatic
    @Singleton
    @Provides
    fun provideGetContentHubItemsUseCase(contentHubRepo: ContentHubRepo): GetContentHubItemsUseCase = GetContentHubItemsUseCase(contentHubRepo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideContentHubRepo(appContext: Context): ContentHubRepo = ContentHubRepo(appContext)

    @JvmStatic
    @Singleton
    @Provides
    fun provideLogoManNotificationRepo(appContext: Context): LogoManNotificationRepo = LogoManNotificationRepo(appContext)

    @JvmStatic
    @Singleton
    @Provides
    fun provideGetLogoManNotificationUseCase(
        logoManNotificationRepo: LogoManNotificationRepo,
        missionRepo: MissionRepository
    ): GetLogoManNotificationUseCase = GetLogoManNotificationUseCase(logoManNotificationRepo, missionRepo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideDismissLogoManNotificationUseCase(
        logoManNotificationRepo: LogoManNotificationRepo,
        missionRepo: MissionRepository
    ): DismissLogoManNotificationUseCase =
            DismissLogoManNotificationUseCase(logoManNotificationRepo, missionRepo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideIsMsrpAvailableUseCase(missionRepo: MissionRepository) = IsMsrpAvailableUseCase(missionRepo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideIsShoppingButtonEnabledUseCase(shoppingSearchRepository: ShoppingSearchRepository): IsShoppingButtonEnabledUseCase =
            IsShoppingButtonEnabledUseCase(shoppingSearchRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideCheckFirstRunUseCase(appContext: Context): CheckFirstRunUseCase =
            CheckFirstRunUseCase(appContext)

    @JvmStatic
    @Singleton
    @Provides
    fun provideCheckLiteUpdateUseCase(appContext: Context): CheckLiteUpdate =
            CheckLiteUpdate(appContext)

    @JvmStatic
    @Singleton
    @Provides
    fun provideCompleteFirstRunUseCase(appContext: Context): CompleteFirstRunUseCase =
            CompleteFirstRunUseCase(appContext)

    @JvmStatic
    @Singleton
    @Provides
    fun provideCompleteLiteUpdateUseCase(appContext: Context): CompleteLiteUpdate =
            CompleteLiteUpdate(appContext)
}