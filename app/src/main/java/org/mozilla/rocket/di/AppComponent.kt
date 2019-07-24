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
import dagger.Component
import org.mozilla.rocket.content.di.ContentModule
import org.mozilla.rocket.content.news.NewsFragment
import org.mozilla.rocket.content.news.NewsSettingFragment
import org.mozilla.rocket.content.news.NewsTabFragment
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
        ContentModule::class
    ]
)
interface AppComponent {
    fun appContext(): Context

    fun inject(newsSettingFragment: NewsSettingFragment)

    fun inject(newsTabFragment: NewsTabFragment)

    fun inject(newsFragment: NewsFragment)

    fun inject(newsTabFragment: org.mozilla.rocket.content.news_v2.NewsTabFragment)

    fun inject(newsFragment: org.mozilla.rocket.content.news_v2.NewsFragment)
}