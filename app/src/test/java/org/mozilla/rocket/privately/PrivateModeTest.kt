/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.rocket.privately

import android.preference.PreferenceManager
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment


@RunWith(RobolectricTestRunner::class)
class PrivateModeTest {

    @Test
    fun `Private mode should be disable if it's pref off`() {
        PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application)
                .edit()
                .putBoolean(PrivateMode.PREF_KEY_PRIVATE_MODE_ENABLED, true)
                .apply()
        assertTrue(PrivateMode.isEnable(RuntimeEnvironment.application))
    }
}