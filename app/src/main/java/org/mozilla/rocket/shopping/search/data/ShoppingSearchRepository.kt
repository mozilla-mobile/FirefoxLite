package org.mozilla.rocket.shopping.search.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ShoppingSearchRepository(
    private val remoteDataSource: ShoppingSearchDataSource,
    private val localDataSource: ShoppingSearchDataSource
) {
    private val shoppingSitesData: MutableLiveData<List<ShoppingSite>> by lazy {
        MutableLiveData<List<ShoppingSite>>().apply {
            CoroutineScope(Dispatchers.IO).launch {
                postValue(getShoppingSites())
            }
        }
    }

    private fun getShoppingSites(): List<ShoppingSite> {
        val remoteShoppingSites = remoteDataSource.getShoppingSites()
        val localShoppingSites = localDataSource.getShoppingSites()

        return if (localShoppingSites.isEmpty() && remoteShoppingSites.isNotEmpty()) {
            localDataSource.updateShoppingSites(remoteShoppingSites)
            remoteShoppingSites
        } else if (localShoppingSites.isNotEmpty()) {
            val mergedShoppingSites = arrayListOf<ShoppingSite>()
            if (shouldMergeShoppingSites(remoteShoppingSites, localShoppingSites)) {
                mergedShoppingSites.addAll(getMergedShoppingSites(remoteShoppingSites, localShoppingSites))
                updateShoppingSites(mergedShoppingSites)
            } else {
                mergedShoppingSites.addAll(localShoppingSites)
            }
            syncRemoteSettingsToLocalSites(remoteShoppingSites, localShoppingSites)
            mergedShoppingSites
        } else {
            remoteDataSource.getDefaultShoppingSites()
        }
    }

    fun getHomeShoppingSearchEnabledGroups(): List<HomeShoppingSearchEnabledGroup>? =
            remoteDataSource.getHomeShoppingSearchEnabledGroups()

    fun getShoppingSitesData(): LiveData<List<ShoppingSite>> = shoppingSitesData

    fun refreshShoppingSites() {
        shoppingSitesData.postValue(getShoppingSites())
    }

    fun updateShoppingSites(shoppingSites: List<ShoppingSite>) {
        localDataSource.updateShoppingSites(shoppingSites)
        shoppingSitesData.postValue(shoppingSites)
    }

    fun shouldEnableTurboMode() = localDataSource.shouldEnableTurboMode()

    fun shouldShowSearchResultOnboarding() = localDataSource.shouldShowSearchResultOnboarding()

    fun setSearchResultOnboardingIsShown() = localDataSource.setSearchResultOnboardingIsShown()

    fun getSearchPromptMessageShowCount() = localDataSource.getSearchPromptMessageShowCount()

    fun setSearchPromptMessageShowCount(count: Int) = localDataSource.setSearchPromptMessageShowCount(count)

    fun getSearchDescription() = localDataSource.getSearchDescription()

    fun getSearchLogoManImageUrl() = remoteDataSource.getSearchLogoManImageUrl()

    private fun shouldMergeShoppingSites(remoteShoppingSites: List<ShoppingSite>, localShoppingSites: List<ShoppingSite>): Boolean {
        if (remoteShoppingSites.size != localShoppingSites.size) {
            return true
        }

        val remoteSites = remoteShoppingSites.sortedBy { it.title }
        val localSites = localShoppingSites.sortedBy { it.title }
        remoteSites.forEachIndexed { index, remoteSite ->
            if (!remoteSite.contentEquals(localSites[index])) {
                return true
            }
        }

        return false
    }

    private fun getMergedShoppingSites(remoteShoppingSites: List<ShoppingSite>, localShoppingSites: List<ShoppingSite>): List<ShoppingSite> {
        val sitesToAdd = arrayListOf<ShoppingSite>()
        for (remoteSite in remoteShoppingSites) {
            var matched = false
            for (localSite in localShoppingSites) {
                if (localSite.contentEquals(remoteSite)) {
                    matched = true
                    break
                }
            }
            if (!matched) {
                sitesToAdd.add(remoteSite)
            }
        }

        val sitesToDelete = arrayListOf<ShoppingSite>()
        for (localSite in localShoppingSites) {
            var matched = false
            for (remoteSite in remoteShoppingSites) {
                if (localSite.contentEquals(remoteSite)) {
                    matched = true
                    break
                }
            }
            if (!matched) {
                sitesToDelete.add(localSite)
            }
        }

        val mergedSites = arrayListOf<ShoppingSite>()
        mergedSites.addAll(localShoppingSites)
        mergedSites.removeAll(sitesToDelete)
        mergedSites.addAll(sitesToAdd)

        return mergedSites
    }

    private fun syncRemoteSettingsToLocalSites(remoteShoppingSites: List<ShoppingSite>, localShoppingSites: List<ShoppingSite>) {
        for (remoteSite in remoteShoppingSites) {
            for (localSite in localShoppingSites) {
                if (localSite.contentEquals(remoteSite)) {
                    localSite.showPrompt = remoteSite.showPrompt
                }
            }
        }
    }
}
