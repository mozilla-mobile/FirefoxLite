package org.mozilla.rocket.content.di

import dagger.Module
import dagger.Provides
import org.mozilla.rocket.content.common.ui.ContentTabBottomBarViewModel
import org.mozilla.rocket.content.ecommerce.di.ShoppingModule
import org.mozilla.rocket.content.games.di.GamesModule
import org.mozilla.rocket.content.news.di.NewsModule

@Module(includes = [GamesModule::class, ShoppingModule::class, NewsModule::class])
object ContentModule {

    @JvmStatic
    @Provides
    fun provideContentTabBottomBarViewModel(): ContentTabBottomBarViewModel = ContentTabBottomBarViewModel()
}
