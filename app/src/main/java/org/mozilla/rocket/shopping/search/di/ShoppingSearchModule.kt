package org.mozilla.rocket.shopping.search.di

import dagger.Module
import dagger.Provides
import org.mozilla.rocket.shopping.search.data.KeywordSuggestionRepository
import org.mozilla.rocket.shopping.search.data.ShoppingSearchSiteRepository
import org.mozilla.rocket.shopping.search.domain.FetchKeywordSuggestionUseCase
import org.mozilla.rocket.shopping.search.domain.SearchShoppingSiteUseCase
import org.mozilla.rocket.shopping.search.ui.ShoppingSearchBottomBarViewModel
import org.mozilla.rocket.shopping.search.ui.ShoppingSearchKeywordInputViewModel
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
    fun provideShoppingSearchSiteRepository(): ShoppingSearchSiteRepository = ShoppingSearchSiteRepository()

    @JvmStatic
    @Singleton
    @Provides
    fun provideSearchShoppingSiteUseCase(repo: ShoppingSearchSiteRepository) = SearchShoppingSiteUseCase(repo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideShoppingSearchResultViewModel(usecase: SearchShoppingSiteUseCase): ShoppingSearchResultViewModel =
        ShoppingSearchResultViewModel(usecase)

    @JvmStatic
    @Singleton
    @Provides
    fun provideShoppingSearchBottomBarViewModel(): ShoppingSearchBottomBarViewModel =
        ShoppingSearchBottomBarViewModel()
}