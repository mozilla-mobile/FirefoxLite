/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.rocket.tabs

import androidx.annotation.IntDef

object SiteIdentity {
    const val UNKNOWN = 0
    const val INSECURE = 1
    const val SECURE = 2

    @IntDef(UNKNOWN, INSECURE, SECURE)
    annotation class SecurityState
}
