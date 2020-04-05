package org.mozilla.rocket.shopping.search.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.mozilla.rocket.shopping.search.data.KeywordSuggestionRepository
import org.mozilla.rocket.shopping.search.data.ShoppingSearchLocalDataSource
import org.mozilla.rocket.shopping.search.data.ShoppingSearchRemoteDataSource
import org.mozilla.rocket.shopping.search.data.ShoppingSearchRepository
import org.mozilla.rocket.shopping.search.domain.FetchKeywordSuggestionUseCase
import org.mozilla.rocket.shopping.search.domain.GetSearchDescriptionUseCase
import org.mozilla.rocket.shopping.search.domain.GetSearchLogoManImageUrlUseCase
import org.mozilla.rocket.shopping.search.domain.GetSearchPromptMessageShowCountUseCase
import org.mozilla.rocket.shopping.search.domain.GetShoppingSearchSitesUseCase
import org.mozilla.rocket.shopping.search.domain.GetShoppingSitesUseCase
import org.mozilla.rocket.shopping.search.domain.SetSearchPromptMessageShowCountUseCase
import org.mozilla.rocket.shopping.search.domain.SetSearchResultOnboardingIsShownUseCase
import org.mozilla.rocket.shopping.search.domain.ShouldEnableTurboModeUseCase
import org.mozilla.rocket.shopping.search.domain.ShouldShowSearchResultOnboardingUseCase
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
    fun provideKeywordSuggestionRepository(appContext: Context): KeywordSuggestionRepository = KeywordSuggestionRepository(appContext)

    @JvmStatic
    @Singleton
    @Provides
    fun provideFetchKeywordSuggestionUseCase(repo: KeywordSuggestionRepository): FetchKeywordSuggestionUseCase =
        FetchKeywordSuggestionUseCase(repo)

    @JvmStatic
    @Provides
    fun provideShoppingSearchKeywordInputViewModel(
        fetchKeywordUseCase: FetchKeywordSuggestionUseCase,
        getSearchDescriptionUseCase: GetSearchDescriptionUseCase,
        getSearchLogoManImageUrlUseCase: GetSearchLogoManImageUrlUseCase
    ): ShoppingSearchKeywordInputViewModel =
        ShoppingSearchKeywordInputViewModel(
            fetchKeywordUseCase,
            getSearchDescriptionUseCase,
            getSearchLogoManImageUrlUseCase
        )

    @JvmStatic
    @Singleton
    @Provides
    fun provideShoppingSearchRemoteDataSource(): ShoppingSearchRemoteDataSource =
        ShoppingSearchRemoteDataSource()

    @JvmStatic
    @Singleton
    @Provides
    fun provideShoppingSearchLocalDataSource(context: Context): ShoppingSearchLocalDataSource =
        ShoppingSearchLocalDataSource(context)

    @JvmStatic
    @Singleton
    @Provides
    fun provideShoppingSearchRepository(
        shoppingSearchRemoteDataSource: ShoppingSearchRemoteDataSource,
        shoppingSearchLocalDataSource: ShoppingSearchLocalDataSource
    ): ShoppingSearchRepository = ShoppingSearchRepository(shoppingSearchRemoteDataSource, shoppingSearchLocalDataSource)

    @JvmStatic
    @Singleton
    @Provides
    fun provideSearchShoppingSiteUseCase(repo: ShoppingSearchRepository) = GetShoppingSearchSitesUseCase(repo)

    @JvmStatic
    @Provides
    fun provideShoppingSearchResultViewModel(
        getShoppingSearchSitesUseCase: GetShoppingSearchSitesUseCase,
        shouldEnableTurboModeUseCase: ShouldEnableTurboModeUseCase
    ): ShoppingSearchResultViewModel =
        ShoppingSearchResultViewModel(
            getShoppingSearchSitesUseCase,
            shouldEnableTurboModeUseCase
        )

    @JvmStatic
    @Provides
    fun provideShoppingSearchBottomBarViewModel(): ShoppingSearchBottomBarViewModel =
        ShoppingSearchBottomBarViewModel()

    @JvmStatic
    @Singleton
    @Provides
    fun providePreferencesShoppingSiteUseCase(repo: ShoppingSearchRepository) = GetShoppingSitesUseCase(repo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideSaveListToPreferenceUseCase(repo: ShoppingSearchRepository) = UpdateShoppingSitesUseCase(repo)

    @JvmStatic
    @Provides
    fun provideShoppingSearchPreferencesViewModel(usecase: GetShoppingSitesUseCase, saveUseCase: UpdateShoppingSitesUseCase): ShoppingSearchPreferencesViewModel =
        ShoppingSearchPreferencesViewModel(usecase, saveUseCase)

    @JvmStatic
    @Singleton
    @Provides
    fun provideShouldShowSearchResultOnboardingUseCase(repo: ShoppingSearchRepository): ShouldShowSearchResultOnboardingUseCase =
        ShouldShowSearchResultOnboardingUseCase(repo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideSetSearchResultOnboardingIsShownUseCase(repo: ShoppingSearchRepository): SetSearchResultOnboardingIsShownUseCase =
        SetSearchResultOnboardingIsShownUseCase(repo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideShouldEnableTurboModeUseCase(repo: ShoppingSearchRepository): ShouldEnableTurboModeUseCase =
        ShouldEnableTurboModeUseCase(repo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideGetSearchPromptMessageShowCountUseCase(repo: ShoppingSearchRepository): GetSearchPromptMessageShowCountUseCase =
        GetSearchPromptMessageShowCountUseCase(repo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideSetSearchPromptMessageShowCountUseCase(repo: ShoppingSearchRepository): SetSearchPromptMessageShowCountUseCase =
        SetSearchPromptMessageShowCountUseCase(repo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideGetSearchDescriptionUseCase(repo: ShoppingSearchRepository): GetSearchDescriptionUseCase =
        GetSearchDescriptionUseCase(repo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideGetSearchLogoManImageUrlUseCase(repo: ShoppingSearchRepository): GetSearchLogoManImageUrlUseCase =
        GetSearchLogoManImageUrlUseCase(repo)
}
