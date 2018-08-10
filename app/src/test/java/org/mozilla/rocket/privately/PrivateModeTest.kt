/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.rocket.privately

import android.preference.PreferenceManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.utils.AppConfigWrapper
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment


@RunWith(RobolectricTestRunner::class)
class PrivateModeTest {

    @Test
    fun `Private mode is default off`() {
        assertFalse(PrivateMode.isEnable(RuntimeEnvironment.application))
    }
}