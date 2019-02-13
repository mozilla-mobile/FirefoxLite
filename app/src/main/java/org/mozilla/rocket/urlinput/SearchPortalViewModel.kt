package org.mozilla.rocket.urlinput

import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.ViewModel

class SearchPortalViewModel(repository: SearchPortalRepository) : ViewModel() {

    val searchPortalObservable = MediatorLiveData<ArrayList<SearchPortal>>()
    private val liveGlobal = repository.fetchGlobal()
    private val liveLocale = repository.fetchLocale()

    init {
        searchPortalObservable.addSource(liveGlobal) {
            mergePortals()
        }
        searchPortalObservable.addSource(liveLocale) {
            mergePortals()
        }
    }

    private fun mergePortals() {
        val result = ArrayList<SearchPortal>()
        if (liveGlobal.value != null) {
            result.addAll(liveGlobal.value!!)
        }
        if (liveLocale.value != null) {
            result.addAll(liveLocale.value!!)
        }
        searchPortalObservable.value = result
    }
}