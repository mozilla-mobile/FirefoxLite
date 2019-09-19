package org.mozilla.rocket.shopping.search.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.mozilla.rocket.shopping.search.data.KeywordSuggestionRepository
import org.mozilla.rocket.shopping.search.data.OnboardingSharedPreferenceRepository
import org.mozilla.rocket.shopping.search.data.ShoppingSearchRepository
import org.mozilla.rocket.shopping.search.domain.CheckContentSwitchOnboardingFirstRunUseCase
import org.mozilla.rocket.shopping.search.domain.CheckOnboardingFirstRunUseCase
import org.mozilla.rocket.shopping.search.domain.CompleteContentSwitchOnboardingFirstRunUseCase
import org.mozilla.rocket.shopping.search.domain.CompleteOnboardingFirstRunUseCase
import org.mozilla.rocket.shopping.search.domain.FetchKeywordSuggestionUseCase
import org.mozilla.rocket.shopping.search.domain.GetShoppingSearchSitesUseCase
import org.mozilla.rocket.shopping.search.domain.GetShoppingSitesUseCase
import org.mozilla.rocket.shopping.search.domain.UpdateShoppingSitesUseCase
import org.mozilla.rocket.shopping.search.ui.ShoppingSearchBottomBarViewModel
import org.mozilla.rocket.shopping.search.ui.ShoppingSearchKeywordInputViewModel
import org.mozilla.rocket.shopping.search.ui.ShoppingSearchPreferencesViewModel
import org.mozilla.rocket.shopping.search.ui.ShoppingSearchContentSwitchOnboardingViewModel
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
    @Provides
    fun provideShoppingSearchKeywordInputViewModel(
        fetchKeywordUseCase: FetchKeywordSuggestionUseCase,
        checkUseCase: CheckOnboardingFirstRunUseCase,
        completeUseCase: CompleteOnboardingFirstRunUseCase
    ): ShoppingSearchKeywordInputViewModel =
        ShoppingSearchKeywordInputViewModel(fetchKeywordUseCase, checkUseCase, completeUseCase)

    @JvmStatic
    @Singleton
    @Provides
    fun provideShoppingSearchSiteRepository(appContext: Context): ShoppingSearchRepository = ShoppingSearchRepository(appContext)

    @JvmStatic
    @Singleton
    @Provides
    fun provideSearchShoppingSiteUseCase(repo: ShoppingSearchRepository) = GetShoppingSearchSitesUseCase(repo)

    @JvmStatic
    @Provides
    fun provideShoppingSearchResultViewModel(
        usecase: GetShoppingSearchSitesUseCase,
        checkUseCase: CheckContentSwitchOnboardingFirstRunUseCase,
        completeUseCase: CompleteContentSwitchOnboardingFirstRunUseCase
    ): ShoppingSearchResultViewModel =
        ShoppingSearchResultViewModel(usecase, checkUseCase, completeUseCase)

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
    fun provideOnboardingSharedPreferenceRepository(appContext: Context): OnboardingSharedPreferenceRepository = OnboardingSharedPreferenceRepository(appContext)

    @JvmStatic
    @Singleton
    @Provides
    fun provideContentSwitchCheckOnboardingFirstRunUseCase(repo: OnboardingSharedPreferenceRepository): CheckContentSwitchOnboardingFirstRunUseCase =
        CheckContentSwitchOnboardingFirstRunUseCase(repo)

    @JvmStatic
    @Provides
    fun provideShoppingSearchOnboardingViewModel(): ShoppingSearchContentSwitchOnboardingViewModel =
        ShoppingSearchContentSwitchOnboardingViewModel()

    @JvmStatic
    @Singleton
    @Provides
    fun provideContentSwitchCompleteOnboardingFirstRunUseCase(repo: OnboardingSharedPreferenceRepository): CompleteContentSwitchOnboardingFirstRunUseCase =
        CompleteContentSwitchOnboardingFirstRunUseCase(repo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideCheckOnboardingFirstRunUseCase(repo: OnboardingSharedPreferenceRepository): CheckOnboardingFirstRunUseCase =
        CheckOnboardingFirstRunUseCase(repo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideCompleteOnboardingFirstRunUseCase(repo: OnboardingSharedPreferenceRepository): CompleteOnboardingFirstRunUseCase =
        CompleteOnboardingFirstRunUseCase(repo)
}