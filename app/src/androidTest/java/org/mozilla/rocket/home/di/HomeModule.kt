package org.mozilla.rocket.home.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.mockito.Mockito.spy
import org.mozilla.focus.tabs.tabtray.TabTrayViewModel
import org.mozilla.focus.utils.FirebaseHelper
import org.mozilla.focus.utils.NewFeatureNotice
import org.mozilla.focus.utils.Settings
import org.mozilla.rocket.chrome.domain.ShouldShowNewMenuItemHintUseCase
import org.mozilla.rocket.home.HomeViewModel
import org.mozilla.rocket.home.contenthub.data.ContentHubRepo
import org.mozilla.rocket.home.contenthub.domain.GetContentHubItemsUseCase
import org.mozilla.rocket.home.contenthub.domain.ReadContentHubItemUseCase
import org.mozilla.rocket.home.contenthub.domain.SetContentHubEnabledUseCase
import org.mozilla.rocket.home.contenthub.domain.ShouldShowContentHubItemTextUseCase
import org.mozilla.rocket.home.contenthub.domain.ShouldShowContentHubUseCase
import org.mozilla.rocket.home.data.ContentPrefRepo
import org.mozilla.rocket.home.domain.IsHomeScreenShoppingButtonEnabledUseCase
import org.mozilla.rocket.home.domain.SetContentPrefUseCase
import org.mozilla.rocket.home.logoman.data.LogoManNotificationRepo
import org.mozilla.rocket.home.logoman.domain.DismissLogoManNotificationUseCase
import org.mozilla.rocket.home.logoman.domain.GetLogoManNotificationUseCase
import org.mozilla.rocket.home.logoman.domain.LastReadLogoManNotificationUseCase
import org.mozilla.rocket.home.onboarding.domain.SetShoppingSearchOnboardingIsShownUseCase
import org.mozilla.rocket.home.onboarding.domain.SetThemeOnboardingIsShownUseCase
import org.mozilla.rocket.home.onboarding.domain.ShouldShowShoppingSearchOnboardingUseCase
import org.mozilla.rocket.home.onboarding.domain.ShouldShowThemeOnboardingUseCase
import org.mozilla.rocket.home.topsites.data.PinSiteManager
import org.mozilla.rocket.home.topsites.data.SharedPreferencePinSiteDelegate
import org.mozilla.rocket.home.topsites.data.TopSitesRepo
import org.mozilla.rocket.home.topsites.domain.GetRecommendedSitesUseCase
import org.mozilla.rocket.home.topsites.domain.GetTopSitesUseCase
import org.mozilla.rocket.home.topsites.domain.IsTopSiteFullyPinnedUseCase
import org.mozilla.rocket.home.topsites.domain.PinTopSiteUseCase
import org.mozilla.rocket.home.topsites.domain.RemoveTopSiteUseCase
import org.mozilla.rocket.home.topsites.ui.AddNewTopSitesViewModel
import org.mozilla.rocket.msrp.data.MissionRepository
import org.mozilla.rocket.msrp.domain.CheckInMissionUseCase
import org.mozilla.rocket.msrp.domain.CompleteJoinMissionOnboardingUseCase
import org.mozilla.rocket.msrp.domain.GetContentHubClickOnboardingEventUseCase
import org.mozilla.rocket.msrp.domain.GetIsFxAccountUseCase
import org.mozilla.rocket.msrp.domain.HasUnreadMissionsUseCase
import org.mozilla.rocket.msrp.domain.IsMsrpAvailableUseCase
import org.mozilla.rocket.msrp.domain.LastReadMissionIdUseCase
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
        isTopSiteFullyPinnedUseCase: IsTopSiteFullyPinnedUseCase,
        pinTopSiteUseCase: PinTopSiteUseCase,
        removeTopSiteUseCase: RemoveTopSiteUseCase,
        getContentHubItemsUseCase: GetContentHubItemsUseCase,
        shouldShowContentHubItemTextUseCase: ShouldShowContentHubItemTextUseCase,
        readContentHubItemUseCase: ReadContentHubItemUseCase,
        getLogoManNotificationUseCase: GetLogoManNotificationUseCase,
        lastReadLogoManNotificationUseCase: LastReadLogoManNotificationUseCase,
        lastReadMissionIdUseCase: LastReadMissionIdUseCase,
        dismissLogoManNotificationUseCase: DismissLogoManNotificationUseCase,
        isMsrpAvailableUseCase: IsMsrpAvailableUseCase,
        isHomeScreenShoppingButtonEnabledUseCase: IsHomeScreenShoppingButtonEnabledUseCase,
        checkInMissionUseCase: CheckInMissionUseCase,
        completeJoinMissionOnboardingUseCase: CompleteJoinMissionOnboardingUseCase,
        getContentHubClickOnboardingEventUseCase: GetContentHubClickOnboardingEventUseCase,
        refreshMissionsUseCase: RefreshMissionsUseCase,
        hasUnreadMissionsUseCase: HasUnreadMissionsUseCase,
        getIsFxAccountUseCase: GetIsFxAccountUseCase,
        shouldShowShoppingSearchOnboardingUseCase: ShouldShowShoppingSearchOnboardingUseCase,
        setShoppingSearchOnboardingIsShownUseCase: SetShoppingSearchOnboardingIsShownUseCase,
        shouldShowNewMenuItemHintUseCase: ShouldShowNewMenuItemHintUseCase,
        shouldShowContentHubUseCase: ShouldShowContentHubUseCase,
        shouldShowThemeOnboardingUseCase: ShouldShowThemeOnboardingUseCase,
        setThemeOnboardingIsShownUseCase: SetThemeOnboardingIsShownUseCase
    ): HomeViewModel = HomeViewModel(
        settings,
        getTopSitesUseCase,
        isTopSiteFullyPinnedUseCase,
        pinTopSiteUseCase,
        removeTopSiteUseCase,
        getContentHubItemsUseCase,
        shouldShowContentHubItemTextUseCase,
        readContentHubItemUseCase,
        getLogoManNotificationUseCase,
        lastReadLogoManNotificationUseCase,
        lastReadMissionIdUseCase,
        dismissLogoManNotificationUseCase,
        isMsrpAvailableUseCase,
        isHomeScreenShoppingButtonEnabledUseCase,
        checkInMissionUseCase,
        completeJoinMissionOnboardingUseCase,
        getContentHubClickOnboardingEventUseCase,
        refreshMissionsUseCase,
        hasUnreadMissionsUseCase,
        getIsFxAccountUseCase,
        shouldShowShoppingSearchOnboardingUseCase,
        setShoppingSearchOnboardingIsShownUseCase,
        shouldShowNewMenuItemHintUseCase,
        shouldShowContentHubUseCase,
        shouldShowThemeOnboardingUseCase,
        setThemeOnboardingIsShownUseCase
    )

    @JvmStatic
    @Singleton
    @Provides
    fun provideGetTopSitesUseCase(
        topSitesRepo: TopSitesRepo,
        contentPrefRepo: ContentPrefRepo
    ): GetTopSitesUseCase = spy(GetTopSitesUseCase(topSitesRepo, contentPrefRepo))

    @JvmStatic
    @Singleton
    @Provides
    fun provideIsTopSiteFullyPinnedUseCase(topSitesRepo: TopSitesRepo): IsTopSiteFullyPinnedUseCase = IsTopSiteFullyPinnedUseCase(topSitesRepo)

    @JvmStatic
    @Singleton
    @Provides
    fun providePinTopSiteUseCase(topSitesRepo: TopSitesRepo): PinTopSiteUseCase = PinTopSiteUseCase(topSitesRepo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideRemoveTopSiteUseCase(
        topSitesRepo: TopSitesRepo,
        contentPrefRepo: ContentPrefRepo
    ): RemoveTopSiteUseCase = RemoveTopSiteUseCase(topSitesRepo, contentPrefRepo)

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
    fun provideReadContentHubItemUseCase(contentHubRepo: ContentHubRepo): ReadContentHubItemUseCase = ReadContentHubItemUseCase(contentHubRepo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideShouldShowContentHubItemTextUseCase(): ShouldShowContentHubItemTextUseCase =
            ShouldShowContentHubItemTextUseCase(FirebaseHelper.getFirebase())

    @JvmStatic
    @Provides
    fun provideSetContentHubEnabledUseCase(contentHubRepo: ContentHubRepo): SetContentHubEnabledUseCase =
            SetContentHubEnabledUseCase(contentHubRepo)

    @JvmStatic
    @Provides
    fun provideShouldShowContentHubUseCase(contentHubRepo: ContentHubRepo): ShouldShowContentHubUseCase =
            ShouldShowContentHubUseCase(contentHubRepo)

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
    fun provideLastReadLogoManNotificationUseCase(
        logoManNotificationRepo: LogoManNotificationRepo
    ): LastReadLogoManNotificationUseCase = LastReadLogoManNotificationUseCase(logoManNotificationRepo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideLastReadMissionIdUseCase(
        missionRepo: MissionRepository
    ): LastReadMissionIdUseCase = LastReadMissionIdUseCase(missionRepo)

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
    fun provideIsHomeScreenShoppingButtonEnabledUseCase(shoppingSearchRepository: ShoppingSearchRepository): IsHomeScreenShoppingButtonEnabledUseCase =
            IsHomeScreenShoppingButtonEnabledUseCase(shoppingSearchRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideShouldShowShoppingSearchOnboardingUseCase(
        shoppingSearchRepository: ShoppingSearchRepository,
        newFeatureNotice: NewFeatureNotice
    ): ShouldShowShoppingSearchOnboardingUseCase =
            ShouldShowShoppingSearchOnboardingUseCase(shoppingSearchRepository, newFeatureNotice)

    @JvmStatic
    @Singleton
    @Provides
    fun provideSetShoppingSearchOnboardingIsShownUseCase(newFeatureNotice: NewFeatureNotice): SetShoppingSearchOnboardingIsShownUseCase =
            SetShoppingSearchOnboardingIsShownUseCase(newFeatureNotice)

    @JvmStatic
    @Provides
    fun provideTabTrayViewModel(): TabTrayViewModel =
            TabTrayViewModel()

    @JvmStatic
    @Singleton
    @Provides
    fun provideGetRecommendedSitesUseCase(topSitesRepo: TopSitesRepo): GetRecommendedSitesUseCase =
            GetRecommendedSitesUseCase(topSitesRepo)

    @JvmStatic
    @Provides
    fun provideAddNewTopSitesViewModel(
        getRecommendedSitesUseCase: GetRecommendedSitesUseCase,
        pinTopSiteUseCase: PinTopSiteUseCase
    ): AddNewTopSitesViewModel =
            AddNewTopSitesViewModel(getRecommendedSitesUseCase, pinTopSiteUseCase)

    @JvmStatic
    @Provides
    fun provideContentPrefRepo(appContext: Context): ContentPrefRepo = ContentPrefRepo(appContext)

    @JvmStatic
    @Provides
    fun provideSetContentPrefUseCase(contentPrefRepo: ContentPrefRepo): SetContentPrefUseCase =
            SetContentPrefUseCase(contentPrefRepo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideShouldShowThemeOnboardingUseCase(newFeatureNotice: NewFeatureNotice): ShouldShowThemeOnboardingUseCase =
            ShouldShowThemeOnboardingUseCase(newFeatureNotice)

    @JvmStatic
    @Singleton
    @Provides
    fun provideSetThemeOnboardingIsShownUseCase(newFeatureNotice: NewFeatureNotice): SetThemeOnboardingIsShownUseCase =
            SetThemeOnboardingIsShownUseCase(newFeatureNotice)
}