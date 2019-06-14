/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.rocket.urlinput

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel

class QuickSearchViewModel(repository: QuickSearchRepository) : ViewModel() {

    val quickSearchObservable = MediatorLiveData<ArrayList<QuickSearch>>()
    private val liveGlobal = repository.fetchGlobal()
    private val liveLocale = repository.fetchLocale()

    init {
        quickSearchObservable.addSource(liveGlobal) {
            mergeEngines()
        }
        quickSearchObservable.addSource(liveLocale) {
            mergeEngines()
        }
    }

    private fun mergeEngines() {
        val result = ArrayList<QuickSearch>()
        liveGlobal.value?.let { result.addAll(it) }
        liveLocale.value?.let { result.addAll(it) }
        quickSearchObservable.value = result
    }
}