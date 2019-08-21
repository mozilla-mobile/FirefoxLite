package org.mozilla.rocket.home.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.spy
import org.mozilla.focus.utils.Settings
import org.mozilla.rocket.home.HomeViewModelFactory
import org.mozilla.rocket.home.pinsite.PinSiteManager
import org.mozilla.rocket.home.pinsite.SharedPreferencePinSiteDelegate
import org.mozilla.rocket.home.topsites.data.TopSitesRepo
import org.mozilla.rocket.home.topsites.domain.GetTopSitesUseCase
import org.mozilla.rocket.home.topsites.domain.PinTopSiteUseCase
import org.mozilla.rocket.home.topsites.domain.RemoveTopSiteUseCase
import org.mozilla.rocket.home.topsites.domain.TopSitesConfigsUseCase
import javax.inject.Singleton

@Module
object HomeModule {

    private const val TOP_SITES = "[{\"id\":-1,\"url\":\"file:\\/\\/\\/android_asset\\/gpl.html\",\"title\":\"Sample Top Site\",\"favicon\":\"ic_youtube.png\",\"viewCount\":20,\"lastViewTimestamp\":1517196818119},{\"id\":-3,\"url\":\"http:\\/\\/m.tribunnews.com\\/\",\"title\":\"Tribunnews\",\"favicon\":\"ic_tribunnews.png\",\"viewCount\":18,\"lastViewTimestamp\":1517196818119},{\"id\":-5,\"url\":\"https:\\/\\/m.tokopedia.com\\/\",\"title\":\"Tokopedia\",\"favicon\":\"ic_tokopedia.png\",\"viewCount\":16,\"lastViewTimestamp\":1517196818119},{\"id\":-4,\"url\":\"https:\\/\\/m.facebook.com\\/\",\"title\":\"Facebook\",\"favicon\":\"ic_facebook.png\",\"viewCount\":14,\"lastViewTimestamp\":1517196818119},{\"id\":-8,\"url\":\"https:\\/\\/m.bukalapak.com\\/\",\"title\":\"Bukalapak\",\"favicon\":\"ic_bukalapak.png\",\"viewCount\":12,\"lastViewTimestamp\":1517196818119},{\"id\":-6,\"url\":\"http:\\/\\/m.liputan6.com\\/\",\"title\":\"Liputan6\",\"favicon\":\"ic_liputan6.png\",\"viewCount\":10,\"lastViewTimestamp\":1517196818119},{\"id\":-7,\"url\":\"http:\\/\\/www.kompas.com\\/\",\"title\":\"Kompas\",\"favicon\":\"ic_kompas.png\",\"viewCount\":8,\"lastViewTimestamp\":1517196818119},{\"id\":-9,\"url\":\"https:\\/\\/m.kapanlagi.com\\/\",\"title\":\"Kapanlagi\",\"favicon\":\"ic_kapanlagi.png\",\"viewCount\":6,\"lastViewTimestamp\":1517196818119}]"

    @JvmStatic
    @Singleton
    @Provides
    fun provideHomeViewModelFactory(
        settings: Settings,
        getTopSitesUseCase: GetTopSitesUseCase,
        topSitesConfigsUseCase: TopSitesConfigsUseCase,
        pinTopSiteUseCase: PinTopSiteUseCase,
        removeTopSiteUseCase: RemoveTopSiteUseCase
    ): HomeViewModelFactory = HomeViewModelFactory(
        settings,
        getTopSitesUseCase,
        topSitesConfigsUseCase,
        pinTopSiteUseCase,
        removeTopSiteUseCase
    )

    @JvmStatic
    @Singleton
    @Provides
    fun provideGetTopSitesUseCase(topSitesRepo: TopSitesRepo): GetTopSitesUseCase = GetTopSitesUseCase(topSitesRepo)

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
    ): TopSitesRepo =
            spy(TopSitesRepo(appContext, pinSiteManager)).apply {
                // mock return value during testing
                doReturn(TOP_SITES).`when`(this).getDefaultTopSitesJsonString()
            }

    @JvmStatic
    @Singleton
    @Provides
    fun providePinSiteManager(appContext: Context): PinSiteManager =
            PinSiteManager(SharedPreferencePinSiteDelegate(appContext))
}