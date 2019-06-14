/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.privately

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import android.content.Context
import org.mozilla.focus.FocusApplication
import org.mozilla.focus.R
import org.mozilla.focus.navigation.ScreenNavigator
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.download.SingleLiveEvent

class ShortcutViewModel : ViewModel() {
    val eventPromoteShortcut = SingleLiveEvent<PromotionCallback>()
    val eventShowMessage = SingleLiveEvent<Int>()
    val eventCreateShortcut = SingleLiveEvent<Unit>()

    /**
     * @return Event notifying the view to continue its leaving process
     */
    fun interceptLeavingAndCheckShortcut(context: Context): LiveData<Unit> {
        val appContext = context.applicationContext as FocusApplication
        val count = appContext.settings.privateBrowsingSettings.getPrivateModeLeaveCount() + 1
        val continueLeaveEvent = SingleLiveEvent<Unit>()

        val navigationState = ScreenNavigator.get(context).navigationState
        val isHomeState = MediatorLiveData<Boolean>().apply {
            addSource(navigationState) {
                removeSource(navigationState)
                val isHomeScreen = it?.isHome == true
                value = isHomeScreen
            }
        }

        val createdBefore = (context.applicationContext as FocusApplication)
                .settings
                .privateBrowsingSettings
                .isPrivateShortcutCreatedBefore()

        return Transformations.switchMap(isHomeState) { isHome ->
            if (!isHome) {
                continueLeaveEvent.call()
            } else if (count == PROMOTE_SHORTCUT_COUNT && !createdBefore) {
                eventPromoteShortcut.value = createPromotionCallback(appContext, continueLeaveEvent)
                TelemetryWrapper.showPrivateShortcutPrompt()
            } else {
                increaseCount(appContext)
                continueLeaveEvent.call()
            }

            continueLeaveEvent
        }
    }

    private fun createPromotionCallback(
        context: Context,
        continueLeaveEvent: SingleLiveEvent<Unit>
    ) = object : PromotionCallback {
        override fun onPositive() {
            TelemetryWrapper.clickPrivateShortcutPrompt(TelemetryWrapper.Value.POSITIVE)
            increaseCount(context)
            continueLeaveEvent.call()
            eventCreateShortcut.call()
        }

        override fun onNegative() {
            TelemetryWrapper.clickPrivateShortcutPrompt(TelemetryWrapper.Value.NEGATIVE)
            increaseCount(context)
            continueLeaveEvent.call()
            eventShowMessage.value = R.string.private_browsing_dialog_add_shortcut_no_toast
        }

        override fun onCancel() {
            TelemetryWrapper.clickPrivateShortcutPrompt(TelemetryWrapper.Value.DISMISS)
        }
    }

    private fun increaseCount(context: Context) {
        val app = context.applicationContext as FocusApplication
        app.settings.privateBrowsingSettings.increasePrivateModeLeaveCount()
    }

    interface PromotionCallback {
        fun onPositive()
        fun onNegative()
        fun onCancel()
    }

    companion object {
        private const val PROMOTE_SHORTCUT_COUNT = 3
    }
}
