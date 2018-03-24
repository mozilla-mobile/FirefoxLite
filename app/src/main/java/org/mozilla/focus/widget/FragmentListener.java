/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

/**
 * An interface for communication between fragments and other components.
 */
public interface FragmentListener {

    enum TYPE {
        FRAGMENT_STARTED, // payload is the fragment tag
        FRAGMENT_STOPPED, // payload is the fragment tag
        OPEN_PREFERENCE, // no payload
        SHOW_URL_INPUT, // no payload
        SHOW_MENU, // no payload
        DISMISS_URL_INPUT, // payload is boolean value, true if commitAllowingStateLoss
        UPDATE_MENU, // no payload
        SHOW_TAB_TRAY, // no payload
    }

    void onNotified(@NonNull Fragment from,
                    @NonNull TYPE type,
                    @Nullable Object payload);
}

