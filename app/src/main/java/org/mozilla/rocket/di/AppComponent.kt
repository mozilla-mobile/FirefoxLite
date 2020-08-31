/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mozilla.rocket.di

import android.content.Context
import androidx.annotation.VisibleForTesting
import dagger.Component
import org.mozilla.focus.activity.EditBookmarkActivity
import org.mozilla.focus.activity.MainActivity
import org.mozilla.focus.fragment.BookmarksFragment
import org.mozilla.focus.fragment.BrowserFragment
import org.mozilla.focus.fragment.DownloadsFragment
import org.mozilla.focus.history.BrowsingHistoryFragment
import org.mozilla.focus.persistence.TabsDatabase
import org.mozilla.focus.persistence.di.TabsModule
import org.mozilla.focus.tabs.tabtray.TabTrayFragment
import org.mozilla.focus.urlinput.UrlInputFragment
import org.mozilla.focus.widget.DefaultBrowserPreference
import org.mozilla.rocket.chrome.ChromeViewModel
import org.mozilla.rocket.chrome.di.ChromeModule
import org.mozilla.rocket.content.common.ui.ContentTabActivity
import org.mozilla.rocket.content.common.ui.ContentTabFragment
import org.mozilla.rocket.content.di.ContentModule
import org.mozilla.rocket.content.news.ui.NewsActivity
import org.mozilla.rocket.content.news.ui.NewsFragment
import org.mozilla.rocket.content.news.ui.NewsLanguageSettingFragment
import org.mozilla.rocket.content.news.ui.NewsSettingFragment
import org.mozilla.rocket.content.news.ui.NewsTabFragment
import org.mozilla.rocket.content.news.ui.PersonalizedNewsOnboardingFragment
import org.mozilla.rocket.content.travel.ui.TravelActivity
import org.mozilla.rocket.content.travel.ui.TravelBucketListFragment
import org.mozilla.rocket.content.travel.ui.TravelCityActivity
import org.mozilla.rocket.content.travel.ui.TravelCitySearchActivity
import org.mozilla.rocket.content.travel.ui.TravelExploreFragment
import org.mozilla.rocket.download.data.DownloadCompleteReceiver
import org.mozilla.rocket.download.data.RelocateService
import org.mozilla.rocket.firstrun.FirstrunFragment
import org.mozilla.rocket.firstrun.di.FirstrunModule
import org.mozilla.rocket.fxa.ProfileActivity
import org.mozilla.rocket.home.HomeFragment
import org.mozilla.rocket.home.di.HomeModule
import org.mozilla.rocket.home.topsites.domain.GetTopSitesUseCase
import org.mozilla.rocket.home.topsites.ui.AddNewTopSitesFragment
import org.mozilla.rocket.menu.BrowserMenuDialog
import org.mozilla.rocket.menu.HomeMenuDialog
import org.mozilla.rocket.msrp.di.MissionModule
import org.mozilla.rocket.msrp.ui.ChallengeListFragment
import org.mozilla.rocket.msrp.ui.MissionCouponFragment
import org.mozilla.rocket.msrp.ui.MissionDetailFragment
import org.mozilla.rocket.msrp.ui.RedeemListFragment
import org.mozilla.rocket.msrp.ui.RewardActivity
import org.mozilla.rocket.msrp.ui.RewardFragment
import org.mozilla.rocket.privately.PrivateModeActivity
import org.mozilla.rocket.privately.home.PrivateHomeFragment
import org.mozilla.rocket.shopping.search.di.ShoppingSearchModule
import org.mozilla.rocket.shopping.search.ui.ShoppingSearchActivity
import org.mozilla.rocket.shopping.search.ui.ShoppingSearchKeywordInputFragment
import org.mozilla.rocket.shopping.search.ui.ShoppingSearchPreferencesActivity
import org.mozilla.rocket.shopping.search.ui.ShoppingSearchResultTabFragment
import javax.inject.Singleton

/**
 * Main component of the app, created and persisted in the Application class.
 *
 * Whenever a new module is created, it should be added to the list of modules.
 * [AndroidSupportInjectionModule] is the module from Dagger.Android that helps with the
 * generation and location of subcomponents.
 */
@Singleton
@Component(
    modules = [
        AppModule::class,
        ContentModule::class,
        ChromeModule::class,
        TabsModule::class,
        HomeModule::class,
        ShoppingSearchModule::class,
        MissionModule::class,
        FirstrunModule::class
    ]
)
interface AppComponent {
    fun appContext(): Context

    fun inject(newsSettingFragment: NewsSettingFragment)
    fun inject(newsTabFragment: NewsTabFragment)
    fun inject(newsFragment: NewsFragment)
    fun inject(personalizedNewsOnboardingFragment: PersonalizedNewsOnboardingFragment)
    fun inject(newsLanguageSettingFragment: NewsLanguageSettingFragment)
    fun inject(mainActivity: MainActivity)
    fun inject(editBookmarkActivity: EditBookmarkActivity)
    fun inject(bookmarksFragment: BookmarksFragment)
    fun inject(browserFragment: BrowserFragment)
    fun inject(browserFragment: org.mozilla.rocket.privately.browse.BrowserFragment)
    fun inject(browserFragmentLegacy: org.mozilla.rocket.privately.browse.BrowserFragmentLegacy)
    fun inject(downloadsFragment: DownloadsFragment)
    fun inject(homeFragment: HomeFragment)
    fun inject(tabTrayFragment: TabTrayFragment)
    fun inject(privateHomeFragment: PrivateHomeFragment)
    fun inject(urlInputFragment: UrlInputFragment)
    fun inject(homeMenuDialog: HomeMenuDialog)
    fun inject(browserMenuDialog: BrowserMenuDialog)
    fun inject(browsingHistoryFragment: BrowsingHistoryFragment)
    fun inject(privateModeActivity: PrivateModeActivity)
    fun inject(contentTabActivity: ContentTabActivity)
    fun inject(contentTabFragment: ContentTabFragment)
    fun inject(newsActivity: NewsActivity)
    fun inject(shoppingSearchActivity: ShoppingSearchActivity)
    fun inject(shoppingSearchKeywordInputFragment: ShoppingSearchKeywordInputFragment)
    fun inject(shoppingSearchResultTabFragment: ShoppingSearchResultTabFragment)
    fun inject(missionDetailFragment: MissionDetailFragment)
    fun inject(couponFragment: MissionCouponFragment)
    fun inject(rewardFragment: RewardFragment)
    fun inject(challengeListFragment: ChallengeListFragment)
    fun inject(redeemListFragment: RedeemListFragment)
    fun inject(shoppingSearchPreferencesActivity: ShoppingSearchPreferencesActivity)
    fun inject(rewardActivity: RewardActivity)
    fun inject(profileActivity: ProfileActivity)
    fun inject(travelActivity: TravelActivity)
    fun inject(travelCityActivity: TravelCityActivity)
    fun inject(travelExploreFragment: TravelExploreFragment)
    fun inject(travelCitySearchActivity: TravelCitySearchActivity)
    fun inject(travelBucketListFragment: TravelBucketListFragment)
    fun inject(defaultBrowserPreference: DefaultBrowserPreference)
    fun inject(addNewTopSitesFragment: AddNewTopSitesFragment)
    fun inject(firstrunFragment: FirstrunFragment)
    fun inject(downloadCompleteReceiver: DownloadCompleteReceiver)
    fun inject(relocateService: RelocateService)

    @VisibleForTesting
    fun chromeViewModel(): ChromeViewModel

    @VisibleForTesting
    fun tabsDatabase(): TabsDatabase

    @VisibleForTesting
    fun getTopSitesUseCase(): GetTopSitesUseCase
}