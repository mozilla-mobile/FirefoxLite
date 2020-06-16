package org.mozilla.rocket.home.topsites.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.mozilla.rocket.home.topsites.domain.GetTopSitesUseCase

class AddNewTopSitesViewModel(
    private val getTopSitesUseCase: GetTopSitesUseCase
) : ViewModel(), TopSiteClickListener {

    private val _topSitesItems = MutableLiveData<List<Site>>()
    val topSitesItems: LiveData<List<Site>> = _topSitesItems

    fun requestTopSitesList() {
        getTopSitesList()
    }

    override fun onTopSiteClicked(site: Site, position: Int) = Unit

    override fun onTopSiteLongClicked(site: Site, position: Int): Boolean = false

    private fun getTopSitesList() = viewModelScope.launch {
        _topSitesItems.value = getTopSitesUseCase()
    }
}