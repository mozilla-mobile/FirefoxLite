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
import org.mozilla.focus.activity.MainActivity
import org.mozilla.focus.fragment.BrowserFragment
import org.mozilla.focus.fragment.DownloadsFragment
import org.mozilla.focus.history.BrowsingHistoryFragment
import org.mozilla.focus.home.HomeFragment
import org.mozilla.rocket.home.topsites.data.TopSitesRepo
import org.mozilla.focus.persistence.TabsDatabase
import org.mozilla.focus.persistence.di.TabsModule
import org.mozilla.focus.urlinput.UrlInputFragment
import org.mozilla.rocket.chrome.ChromeViewModel
import org.mozilla.rocket.chrome.di.ChromeModule
import org.mozilla.rocket.content.common.ui.ContentTabActivity
import org.mozilla.rocket.content.common.ui.ContentTabFragment
import org.mozilla.rocket.content.di.ContentModule
import org.mozilla.rocket.content.ecommerce.EcFragment
import org.mozilla.rocket.content.games.ui.BrowserGamesFragment
import org.mozilla.rocket.content.games.ui.GamesActivity
import org.mozilla.rocket.content.news.NewsFragment
import org.mozilla.rocket.content.news.NewsSettingFragment
import org.mozilla.rocket.content.news.NewsTabFragment
import org.mozilla.rocket.home.di.HomeModule
import org.mozilla.rocket.menu.MenuDialog
import org.mozilla.rocket.privately.PrivateModeActivity
import org.mozilla.rocket.privately.home.PrivateHomeFragment
import org.mozilla.rocket.shopping.search.di.ShoppingSearchModule
import org.mozilla.rocket.shopping.search.ui.ShoppingSearchKeywordInputFragment
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
        ShoppingSearchModule::class
    ]
)
interface AppComponent {
    fun appContext(): Context

    fun inject(newsSettingFragment: NewsSettingFragment)
    fun inject(newsTabFragment: NewsTabFragment)
    fun inject(newsFragment: NewsFragment)
    fun inject(mainActivity: MainActivity)
    fun inject(browserFragment: BrowserFragment)
    fun inject(browserFragment: org.mozilla.rocket.privately.browse.BrowserFragment)
    fun inject(downloadsFragment: DownloadsFragment)
    fun inject(homeFragment: HomeFragment)
    fun inject(homeFragment: org.mozilla.rocket.home.HomeFragment)
    fun inject(privateHomeFragment: PrivateHomeFragment)
    fun inject(urlInputFragment: UrlInputFragment)
    fun inject(menuDialog: MenuDialog)
    fun inject(browsingHistoryFragment: BrowsingHistoryFragment)
    fun inject(ecFragment: EcFragment)
    fun inject(privateModeActivity: PrivateModeActivity)
    fun inject(gamesActivity: GamesActivity)
    fun inject(browserGamesFragment: BrowserGamesFragment)
    fun inject(contentTabActivity: ContentTabActivity)
    fun inject(contentTabFragment: ContentTabFragment)
    fun inject(shoppingSearchKeywordInputFragment: ShoppingSearchKeywordInputFragment)

    @VisibleForTesting
    fun chromeViewModel(): ChromeViewModel
    @VisibleForTesting
    fun tabsDatabase(): TabsDatabase
    @VisibleForTesting
    fun topSitesRepo(): TopSitesRepo
}