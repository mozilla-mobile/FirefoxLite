package org.mozilla.rocket.content.ecommerce.di

import dagger.Module
import dagger.Provides
import org.mozilla.rocket.content.ecommerce.ui.ShoppingViewModel
import org.mozilla.rocket.content.ecommerce.data.ShoppingRepo
import javax.inject.Singleton

@Module
object ShoppingModule {

    @JvmStatic
    @Singleton
    @Provides
    fun provideShoppingRepo(): ShoppingRepo = ShoppingRepo()

    @JvmStatic
    @Singleton
    @Provides
    fun provideShoppingViewModel(repo: ShoppingRepo): ShoppingViewModel = ShoppingViewModel(repo)
}