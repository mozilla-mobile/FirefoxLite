/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.rocket.tabs

import android.app.Activity

object TabsSessionProvider {

    @JvmStatic
    fun getOrThrow(activity: Activity?): SessionManager {
        return (activity as? SessionHost)?.getSessionManager()
            ?: throw IllegalArgumentException(
                "activity must implement TabsSessionProvider.SessionHost"
            )
    }

    @JvmStatic
    fun getOrNull(activity: Activity?): SessionManager? {
        return try {
            getOrThrow(activity)
        } catch (e: Exception) {
            null
        }
    }

    interface SessionHost {
        fun getSessionManager(): SessionManager
    }
}
