/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.privately

import android.arch.lifecycle.ViewModel
import android.content.Context
import org.mozilla.focus.FocusApplication
import org.mozilla.focus.R
import org.mozilla.rocket.download.SingleLiveEvent

class ShortcutViewModel : ViewModel() {
    val eventPromoteShortcut = SingleLiveEvent<PromotionCallback>()
    val eventShowMessage = SingleLiveEvent<Int>()
    val eventCreateShortcut = SingleLiveEvent<Unit>()

    /**
     * @return Event notifying the view to continue its leaving process
     */
    fun interceptLeavingAndCheckShortcut(context: Context): SingleLiveEvent<Unit> {
        val app = context.applicationContext as FocusApplication
        val count = app.settings.privateBrowsingSettings.getPrivateModeLeaveCount() + 1
        val continueLeaveEvent = SingleLiveEvent<Unit>()

        if (count == PROMOTE_SHORTCUT_COUNT) {
            eventPromoteShortcut.value = createPromotionCallback(context, continueLeaveEvent)
        } else {
            increaseCount(context)
            continueLeaveEvent.call()
        }

        return continueLeaveEvent
    }

    private fun createPromotionCallback(
        context: Context,
        continueLeaveEvent: SingleLiveEvent<Unit>
    ) = object : PromotionCallback {
        override fun onPositive() {
            increaseCount(context)
            continueLeaveEvent.call()
            eventCreateShortcut.call()
        }

        override fun onNegative() {
            increaseCount(context)
            continueLeaveEvent.call()
            eventShowMessage.value = R.string.private_browsing_dialog_add_shortcut_no_toast
        }

        override fun onCancel() {}
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
