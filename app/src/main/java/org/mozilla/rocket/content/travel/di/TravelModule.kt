package org.mozilla.rocket.content.travel.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.mozilla.rocket.content.travel.data.TravelLocalDataSource
import org.mozilla.rocket.content.travel.data.TravelOnboardingRepository
import org.mozilla.rocket.content.travel.data.TravelRemoteDataSource
import org.mozilla.rocket.content.travel.data.TravelRepository
import org.mozilla.rocket.content.travel.domain.AddToBucketListUseCase
import org.mozilla.rocket.content.travel.domain.CheckIsInBucketListUseCase
import org.mozilla.rocket.content.travel.domain.GetBucketListUseCase
import org.mozilla.rocket.content.travel.domain.GetCityHotelsUseCase
import org.mozilla.rocket.content.travel.domain.GetCityIgUseCase
import org.mozilla.rocket.content.travel.domain.GetCityVideosUseCase
import org.mozilla.rocket.content.travel.domain.GetCityWikiUseCase
import org.mozilla.rocket.content.travel.domain.GetEnglishNameUseCase
import org.mozilla.rocket.content.travel.domain.GetExploreListUseCase
import org.mozilla.rocket.content.travel.domain.RemoveFromBucketListUseCase
import org.mozilla.rocket.content.travel.domain.SearchCityUseCase
import org.mozilla.rocket.content.travel.domain.SetOnboardingHasShownUseCase
import org.mozilla.rocket.content.travel.domain.ShouldShowOnboardingUseCase
import org.mozilla.rocket.content.travel.ui.TravelBucketListViewModel
import org.mozilla.rocket.content.travel.ui.TravelCitySearchViewModel
import org.mozilla.rocket.content.travel.ui.TravelCityViewModel
import org.mozilla.rocket.content.travel.ui.TravelExploreViewModel
import org.mozilla.rocket.content.travel.ui.TravelViewModel
import javax.inject.Singleton

@Module
object TravelModule {

    @JvmStatic
    @Singleton
    @Provides
    fun provideTravelRemoteDataSource(): TravelRemoteDataSource = TravelRemoteDataSource()

    @JvmStatic
    @Singleton
    @Provides
    fun provideTravelLocalDataSource(context: Context): TravelLocalDataSource = TravelLocalDataSource(context)

    @JvmStatic
    @Singleton
    @Provides
    fun provideTravelRepository(
        travelRemoteDataSource: TravelRemoteDataSource,
        travelLocalDataSource: TravelLocalDataSource
    ): TravelRepository = TravelRepository(travelRemoteDataSource, travelLocalDataSource)

    @JvmStatic
    @Singleton
    @Provides
    fun provideGetExploreListUseCase(travelRepository: TravelRepository): GetExploreListUseCase = GetExploreListUseCase(travelRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideGetBucketListUseCase(travelRepository: TravelRepository): GetBucketListUseCase = GetBucketListUseCase(travelRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideSearchCityUseCase(travelRepository: TravelRepository): SearchCityUseCase = SearchCityUseCase(travelRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideGetCityIgUseCase(travelRepository: TravelRepository): GetCityIgUseCase = GetCityIgUseCase(travelRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideGetCityWikiUseCase(travelRepository: TravelRepository): GetCityWikiUseCase = GetCityWikiUseCase(travelRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideGetCityVideosUseCase(travelRepository: TravelRepository): GetCityVideosUseCase = GetCityVideosUseCase(travelRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideGetCityHotelsUseCase(travelRepository: TravelRepository): GetCityHotelsUseCase = GetCityHotelsUseCase(travelRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideCheckIsInBucketListUseCase(travelRepository: TravelRepository): CheckIsInBucketListUseCase = CheckIsInBucketListUseCase(travelRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideAddToBucketListUseCase(travelRepository: TravelRepository): AddToBucketListUseCase = AddToBucketListUseCase(travelRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideRemoveFromBucketListUseCase(travelRepository: TravelRepository): RemoveFromBucketListUseCase = RemoveFromBucketListUseCase(travelRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideGetEnglishNameUseCase(travelRepository: TravelRepository): GetEnglishNameUseCase = GetEnglishNameUseCase(travelRepository)

    @JvmStatic
    @Provides
    fun provideTravelExploreViewModel(getExploreListUseCase: GetExploreListUseCase): TravelExploreViewModel = TravelExploreViewModel(getExploreListUseCase)

    @JvmStatic
    @Provides
    fun provideTravelBucketListViewModel(
        getBucketListUseCase: GetBucketListUseCase,
        removeFromBucketListUseCase: RemoveFromBucketListUseCase
    ): TravelBucketListViewModel = TravelBucketListViewModel(getBucketListUseCase, removeFromBucketListUseCase)

    @JvmStatic
    @Provides
    fun provideTravelCitySearchViewModel(searchCityUseCase: SearchCityUseCase): TravelCitySearchViewModel = TravelCitySearchViewModel(searchCityUseCase)

    @JvmStatic
    @Provides
    fun provideTravelCityViewModel(
        getCityIgUseCase: GetCityIgUseCase,
        getCityWikiUseCase: GetCityWikiUseCase,
        getCityVideosUseCase: GetCityVideosUseCase,
        getCityHotelsUseCase: GetCityHotelsUseCase,
        checkIsInBucketListUseCase: CheckIsInBucketListUseCase,
        addToBucketListUseCase: AddToBucketListUseCase,
        removeFromBucketListUseCase: RemoveFromBucketListUseCase,
        getEnglishNameUseCase: GetEnglishNameUseCase,
        shouldShowOnboardingUseCase: ShouldShowOnboardingUseCase,
        setOnboardingHasShownUseCase: SetOnboardingHasShownUseCase
    ): TravelCityViewModel = TravelCityViewModel(
        getCityIgUseCase,
        getCityWikiUseCase,
        getCityVideosUseCase,
        getCityHotelsUseCase,
        checkIsInBucketListUseCase,
        addToBucketListUseCase,
        removeFromBucketListUseCase,
        getEnglishNameUseCase,
        shouldShowOnboardingUseCase,
        setOnboardingHasShownUseCase
    )

    @JvmStatic
    @Provides
    fun provideTravelViewModel(): TravelViewModel = TravelViewModel()

    @JvmStatic
    @Singleton
    @Provides
    fun provideShouldShowOnboardingUseCase(travelOnboardingRepository: TravelOnboardingRepository): ShouldShowOnboardingUseCase = ShouldShowOnboardingUseCase(travelOnboardingRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideSetOnboardingHasShownUseCase(travelOnboardingRepository: TravelOnboardingRepository): SetOnboardingHasShownUseCase = SetOnboardingHasShownUseCase(travelOnboardingRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideTravelOnboardingRepository(
        appContext: Context
    ): TravelOnboardingRepository = TravelOnboardingRepository(appContext)
}