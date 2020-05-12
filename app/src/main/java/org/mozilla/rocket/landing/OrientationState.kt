/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.landing

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import android.content.pm.ActivityInfo

import org.mozilla.focus.navigation.ScreenNavigator

class OrientationState(
    navigationModel: NavigationModel,
    portraitModel: PortraitModel
) : MediatorLiveData<Int>() {

    private var navigationState: ScreenNavigator.NavigationState? = null
    private var isPortraitRequested: Boolean = false

    init {
        addSource(navigationModel.navigationState) { currentState ->
            currentState?.let {
                navigationState = it
                portraitModel.resetState()
                checkOrientation()
            }
        }

        addSource(portraitModel.isPortraitState) { isPortrait ->
            isPortrait?.let {
                isPortraitRequested = it
                checkOrientation()
            }
        }
    }

    private fun checkOrientation() {
        val navigationState = navigationState ?: return
        val orientation = if ((navigationState.isBrowser || navigationState.isBrowserUrlInput) && !isPortraitRequested) {
            ActivityInfo.SCREEN_ORIENTATION_USER
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        value = orientation
    }
}

interface NavigationModel {
    val navigationState: LiveData<ScreenNavigator.NavigationState>
}

interface PortraitModel {
    val isPortraitState: LiveData<Boolean>
    fun resetState()
}
