/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.landing

import android.app.Dialog
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.support.v4.app.DialogFragment
import android.view.View

import java.util.WeakHashMap

class PortraitStateModel : PortraitModel {
    private val stateData = MutableLiveData<Boolean>()
    private val requests = WeakHashMap<Any, Boolean>()

    override val isPortraitState: LiveData<Boolean>
        get() = stateData

    init {
        stateData.value = false
    }

    @Suppress("unused")
    fun request(view: View) {
        requestWithToken(view)
    }

    @Suppress("unused")
    fun cancelRequest(view: View) {
        cancelRequestWithToken(view)
    }

    fun request(dialog: Dialog) {
        requestWithToken(dialog)
    }

    fun cancelRequest(dialog: Dialog) {
        cancelRequestWithToken(dialog)
    }

    fun request(fragment: DialogFragment) {
        requestWithToken(fragment)
    }

    fun cancelRequest(fragment: DialogFragment) {
        cancelRequestWithToken(fragment)
    }

    override fun resetState() {
        requests.clear()
        checkState()
    }

    private fun requestWithToken(token: Any) {
        requests[token] = true
        checkState()
    }

    private fun cancelRequestWithToken(token: Any) {
        requests.remove(token)
        checkState()
    }

    private fun checkState() {
        var isShown = false
        for (key in requests.keys) {
            if (requests[key] == true) {
                isShown = true
                break
            }
        }

        val currentState = stateData.value
        if (currentState != isShown) {
            stateData.value = isShown
        }
    }
}
