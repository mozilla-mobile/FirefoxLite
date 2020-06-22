package org.mozilla.rocket.home.topsites.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.download.SingleLiveEvent
import org.mozilla.rocket.home.topsites.data.RecommendedSitesResult
import org.mozilla.rocket.home.topsites.domain.GetRecommendedSitesUseCase
import org.mozilla.rocket.home.topsites.domain.PinTopSiteUseCase
import org.mozilla.rocket.home.topsites.domain.PinTopSiteUseCase.PinTopSiteResult
import org.mozilla.rocket.home.topsites.domain.toFixedSite

class AddNewTopSitesViewModel(
    private val getRecommendedSites: GetRecommendedSitesUseCase,
    private val pinTopSite: PinTopSiteUseCase
) : ViewModel(), TopSiteClickListener {

    private val _recommendedSitesResult = MutableLiveData<RecommendedSitesUiResult>()
    val recommendedSitesItems: LiveData<RecommendedSitesUiResult> = _recommendedSitesResult

    val pinTopSiteResult = SingleLiveEvent<PinTopSiteResult>()

    init {
        getRecommendedSitesList()
    }

    override fun onTopSiteClicked(site: Site, position: Int) {
        when (site) {
            is Site.UrlSite -> {
                viewModelScope.launch {
                    pinTopSiteResult.value = pinTopSite(site)
                }
            }
        }
    }

    override fun onTopSiteLongClicked(site: Site, position: Int): Boolean = false

    private fun getRecommendedSitesList() = viewModelScope.launch() {
        _recommendedSitesResult.value = getRecommendedSites().toUiModel()
    }
}

private fun RecommendedSitesResult.toUiModel(): RecommendedSitesUiResult {
    val uiItems = ArrayList<DelegateAdapter.UiModel>()

    if (categories.isNotEmpty()) {
        categories.map {
            uiItems.add(RecommendedSitesUiCategory(it.categoryId, it.categoryName))
            uiItems.addAll(it.sites.toFixedSite())
        }
    }

    return RecommendedSitesUiResult(uiItems)
}

class RecommendedSitesUiResult(
    val items: List<DelegateAdapter.UiModel>
)