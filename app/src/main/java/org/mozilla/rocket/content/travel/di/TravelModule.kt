package org.mozilla.rocket.content.travel.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.mozilla.rocket.content.travel.data.TravelLocalDataSource
import org.mozilla.rocket.content.travel.data.TravelRemoteDataSource
import org.mozilla.rocket.content.travel.data.TravelRepository
import org.mozilla.rocket.content.travel.domain.GetBucketListUseCase
import org.mozilla.rocket.content.travel.domain.GetCityCategoriesUseCase
import org.mozilla.rocket.content.travel.domain.GetCityHotelsUseCase
import org.mozilla.rocket.content.travel.domain.GetCityIgUseCase
import org.mozilla.rocket.content.travel.domain.GetCityVideosUseCase
import org.mozilla.rocket.content.travel.domain.GetCityWikiUseCase
import org.mozilla.rocket.content.travel.domain.GetRunwayItemsUseCase
import org.mozilla.rocket.content.travel.domain.SearchCityUseCase
import org.mozilla.rocket.content.travel.ui.TravelBucketListViewModel
import org.mozilla.rocket.content.travel.ui.TravelCitySearchViewModel
import org.mozilla.rocket.content.travel.ui.TravelCityViewModel
import org.mozilla.rocket.content.travel.ui.TravelExploreViewModel
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
    fun provideGetRunwayItemsUseCase(travelRepository: TravelRepository): GetRunwayItemsUseCase = GetRunwayItemsUseCase(travelRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideGetCityCategoriesUseCase(travelRepository: TravelRepository): GetCityCategoriesUseCase = GetCityCategoriesUseCase(travelRepository)

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
    @Provides
    fun provideTravelExploreViewModel(
        getRunwayItemsUseCase: GetRunwayItemsUseCase,
        getCityCategoriesUseCase: GetCityCategoriesUseCase
    ): TravelExploreViewModel = TravelExploreViewModel(getRunwayItemsUseCase, getCityCategoriesUseCase)

    @JvmStatic
    @Provides
    fun provideTravelBucketListViewModel(getBucketListUseCase: GetBucketListUseCase): TravelBucketListViewModel = TravelBucketListViewModel(getBucketListUseCase)

    @JvmStatic
    @Provides
    fun provideTravelCitySearchViewModel(searchCityUseCase: SearchCityUseCase): TravelCitySearchViewModel = TravelCitySearchViewModel(searchCityUseCase)

    @JvmStatic
    @Provides
    fun provideTravelCityViewModel(
        getCityIgUseCase: GetCityIgUseCase,
        getCityWikiUseCase: GetCityWikiUseCase,
        getCityVideosUseCase: GetCityVideosUseCase,
        getCityHotelsUseCase: GetCityHotelsUseCase
    ): TravelCityViewModel = TravelCityViewModel(getCityIgUseCase, getCityWikiUseCase, getCityVideosUseCase, getCityHotelsUseCase)
}