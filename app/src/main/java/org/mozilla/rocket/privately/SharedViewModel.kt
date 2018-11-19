/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.rocket.privately

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel

/*
* View Model for PrivateModeActivity.
* This is also used for state sharing among PrivateModeActivity and it's fragments.
*
* */
class SharedViewModel : ViewModel() {

    private var isUrlInputShowing = MutableLiveData<Boolean>()
    private val url: MutableLiveData<String> = MutableLiveData()

    fun urlInputState(): MutableLiveData<Boolean> {
        return isUrlInputShowing
    }

    fun setUrl(newUrl: String) {
        url.value = newUrl
    }

    fun getUrl(): LiveData<String> {
        return url
    }
}