/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.landing

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.content.pm.ActivityInfo

import org.mozilla.focus.navigation.ScreenNavigator

class OrientationState(
    navigationModel: NavigationModel,
    portraitModel: PortraitModel
) : MediatorLiveData<Int>() {

    private var currentFragment: String = ""
    private var isPortraitRequested: Boolean = false

    init {
        addSource(navigationModel.navigationState) { topFragmentTag ->
            topFragmentTag?.let {
                currentFragment = it
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
        val orientation = if (isBrowserFragment(currentFragment) && !isPortraitRequested) {
            ActivityInfo.SCREEN_ORIENTATION_USER
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        value = orientation
    }

    private fun isBrowserFragment(tag: String): Boolean {
        return tag == ScreenNavigator.BROWSER_FRAGMENT_TAG
    }
}

interface NavigationModel {
    val navigationState: LiveData<String>
}

interface PortraitModel {
    val isPortraitState: LiveData<Boolean>
    fun resetState()
}
