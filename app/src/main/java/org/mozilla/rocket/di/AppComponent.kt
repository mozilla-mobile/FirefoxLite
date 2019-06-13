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

import dagger.Component
import dagger.android.support.AndroidSupportInjectionModule
import org.mozilla.focus.FocusApplication
import org.mozilla.rocket.content.di.ContentModule
import javax.inject.Singleton
import dagger.BindsInstance
import dagger.Module
import dagger.android.AndroidInjector
import dagger.android.ContributesAndroidInjector
import org.mozilla.rocket.content.news.NewsFragment
import org.mozilla.rocket.content.news.NewsTabFragment

/**
 * Main component of the app, created and persisted in the Application class.
 *
 * Whenever a new module is created, it should be added to the list of modules.
 * [AndroidSupportInjectionModule] is the module from Dagger.Android that helps with the
 * generation and location of subcomponents.
 */
@Singleton
@Component(
    modules = [AndroidSupportInjectionModule::class,
        ContentModule::class,
        FragmentModule::class,
        AppModule::class
    ]
)

interface AppComponent : AndroidInjector<FocusApplication> {

    override fun inject(app: FocusApplication)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: FocusApplication): Builder

        fun build(): AppComponent
    }
}

// move this to another file / sub-component
@Module
abstract class FragmentModule {
    @ContributesAndroidInjector
    internal abstract fun bindNewsTabFragment(): NewsTabFragment

    @ContributesAndroidInjector
    internal abstract fun bindNewsFragment(): NewsFragment
}