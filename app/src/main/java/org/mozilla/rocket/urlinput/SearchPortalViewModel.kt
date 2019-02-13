/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
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
        liveGlobal.value?.let { result.addAll(it) }
        liveLocale.value?.let { result.addAll(it) }
        searchPortalObservable.value = result
    }
}