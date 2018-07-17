/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

/**
 * An interface for communication between fragments and other components.
 */
public interface FragmentListener {

    enum TYPE {
        OPEN_PREFERENCE, // no payload
        OPEN_URL_IN_CURRENT_TAB, // payload is url in String
        OPEN_URL_IN_NEW_TAB, // payload is url in String
        SHOW_URL_INPUT, // no payload
        SHOW_MENU, // no payload
        DISMISS_URL_INPUT, // payload is boolean value, true if commitAllowingStateLoss
        UPDATE_MENU, // no payload
        REFRESH_TOP_SITE, // no payload
        TOGGLE_PRIVATE_MODE, // no payload
        SHOW_TAB_TRAY, // no payload
    }

    void onNotified(@NonNull Fragment from,
                    @NonNull TYPE type,
                    @Nullable Object payload);

    static void notifyParent(Fragment fragment, FragmentListener.TYPE type, Object payload) {
        final Activity activity = fragment.getActivity();
        if (activity instanceof FragmentListener) {
            ((FragmentListener) activity).onNotified(fragment, type, payload);
        }
    }
}

