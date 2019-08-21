package org.mozilla.rocket.shopping.search.di

import dagger.Module
import dagger.Provides
import org.mozilla.rocket.shopping.search.data.KeywordSuggestionRepository
import org.mozilla.rocket.shopping.search.domain.FetchKeywordSuggestionUseCase
import org.mozilla.rocket.shopping.search.ui.ShoppingSearchKeywordInputViewModel
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
}