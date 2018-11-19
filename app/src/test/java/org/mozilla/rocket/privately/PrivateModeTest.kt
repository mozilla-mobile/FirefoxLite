/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.rocket.privately

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.utils.AppConstants
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PrivateModeTest {

    @Test
    fun `Private mode is default off in Release and Beta build type and is default on in Firebase and Debug build type`() {
        if (AppConstants.isReleaseBuild() || AppConstants.isBetaBuild()) {
            Assert.assertFalse(PrivateMode.isEnable(RuntimeEnvironment.application))
        } else {
            Assert.assertTrue(PrivateMode.isEnable(RuntimeEnvironment.application))
        }
    }
}