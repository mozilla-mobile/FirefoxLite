/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.landing

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class PortraitStateModel : PortraitModel {
    private val stateData = MutableLiveData<Boolean>()
    private val requests = HashSet<PortraitComponent>()

    override val isPortraitState: LiveData<Boolean>
        get() = stateData

    init {
        stateData.value = false
    }

    fun request(component: PortraitComponent) {
        requests.add(component)
        checkPortraitState()
    }

    fun cancelRequest(component: PortraitComponent) {
        requests.remove(component)
        checkPortraitState()
    }

    override fun resetState() {
        requests.clear()
        checkPortraitState()
    }

    private fun checkPortraitState() {
        val hasPortraitPage = requests.isNotEmpty()

        val currentState = stateData.value
        if (currentState != hasPortraitPage) {
            stateData.value = hasPortraitPage
        }
    }
}
