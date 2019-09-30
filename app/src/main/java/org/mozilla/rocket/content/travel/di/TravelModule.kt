package org.mozilla.rocket.content.travel.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.mozilla.rocket.content.travel.data.TravelLocalDataSource
import org.mozilla.rocket.content.travel.data.TravelRemoteDataSource
import org.mozilla.rocket.content.travel.data.TravelRepository
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
}