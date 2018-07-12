/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.tabs;

import android.support.annotation.IntDef;

@SuppressWarnings("WeakerAccess")
public class SiteIdentity {
    public static final int UNKNOWN = 0;
    public static final int INSECURE = 1;
    public static final int SECURE = 2;

    @IntDef({UNKNOWN, INSECURE, SECURE})
    public @interface SecurityState {
    }
}
