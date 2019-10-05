package org.mozilla.rocket.content.di

import dagger.Module
import dagger.Provides
import org.mozilla.rocket.content.common.ui.ContentTabBottomBarViewModel
import org.mozilla.rocket.content.common.ui.RunwayViewModel
import org.mozilla.rocket.content.ecommerce.di.ShoppingModule
import org.mozilla.rocket.content.game.di.GameModule
import org.mozilla.rocket.content.news.di.NewsModule

@Module(includes = [GameModule::class, ShoppingModule::class, NewsModule::class])
object ContentModule {

    @JvmStatic
    @Provides
    fun provideContentTabBottomBarViewModel(): ContentTabBottomBarViewModel = ContentTabBottomBarViewModel()

    @JvmStatic
    @Provides
    fun provideRunwayViewModel(): RunwayViewModel = RunwayViewModel()
}
