package org.mozilla.rocket.shopping.search.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.mozilla.rocket.shopping.search.data.KeywordSuggestionRepository
import org.mozilla.rocket.shopping.search.data.ShoppingSearchSiteRepository
import org.mozilla.rocket.shopping.search.domain.FetchKeywordSuggestionUseCase
import org.mozilla.rocket.shopping.search.domain.GetShoppingSearchSitesUseCase
import org.mozilla.rocket.shopping.search.domain.GetShoppingSitesUseCase
import org.mozilla.rocket.shopping.search.domain.UpdateShoppingSitesUseCase
import org.mozilla.rocket.shopping.search.ui.ShoppingSearchBottomBarViewModel
import org.mozilla.rocket.shopping.search.ui.ShoppingSearchKeywordInputViewModel
import org.mozilla.rocket.shopping.search.ui.ShoppingSearchPreferencesViewModel
import org.mozilla.rocket.shopping.search.ui.ShoppingSearchResultViewModel
import javax.inject.Singleton

@Module
object ShoppingSearchModule {

    @JvmStatic
    @Singleton
    @Provides
    fun provideKeywordSuggestionRepository(): KeywordSuggestionRepository = KeywordSuggestionRepository()

    @JvmStatic
    @Singleton
    @Provides
    fun provideFetchKeywordSuggestionUseCase(repo: KeywordSuggestionRepository): FetchKeywordSuggestionUseCase =
        FetchKeywordSuggestionUseCase(repo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideShoppingSearchKeywordInputViewModel(usecase: FetchKeywordSuggestionUseCase): ShoppingSearchKeywordInputViewModel =
        ShoppingSearchKeywordInputViewModel(usecase)

    @JvmStatic
    @Singleton
    @Provides
    fun provideShoppingSearchSiteRepository(appContext: Context): ShoppingSearchSiteRepository = ShoppingSearchSiteRepository(appContext)

    @JvmStatic
    @Singleton
    @Provides
    fun provideSearchShoppingSiteUseCase(repo: ShoppingSearchSiteRepository) = GetShoppingSearchSitesUseCase(repo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideShoppingSearchResultViewModel(usecase: GetShoppingSearchSitesUseCase): ShoppingSearchResultViewModel =
        ShoppingSearchResultViewModel(usecase)

    @JvmStatic
    @Singleton
    @Provides
    fun provideShoppingSearchBottomBarViewModel(): ShoppingSearchBottomBarViewModel =
        ShoppingSearchBottomBarViewModel()

    @JvmStatic
    @Singleton
    @Provides
    fun providePreferencesShoppingSiteUseCase(repo: ShoppingSearchSiteRepository) = GetShoppingSitesUseCase(repo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideSaveListToPreferenceUseCase(repo: ShoppingSearchSiteRepository) = UpdateShoppingSitesUseCase(repo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideShoppingSearchPreferencesViewModel(usecase: GetShoppingSitesUseCase, saveUseCase: UpdateShoppingSitesUseCase): ShoppingSearchPreferencesViewModel =
            ShoppingSearchPreferencesViewModel(usecase, saveUseCase)
}