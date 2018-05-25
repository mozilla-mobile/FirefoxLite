/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.tabs.tabtray;

import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

public class TabTray {
    public static void show(FragmentManager manager) {
        TabTrayFragment.newInstance().show(manager, TabTrayFragment.FRAGMENT_TAG);
    }

    public static void dismiss(FragmentManager manager) {
        Fragment tabTray = manager.findFragmentByTag(TabTrayFragment.FRAGMENT_TAG);
        if (tabTray != null) {
            ((DialogFragment) tabTray).dismissAllowingStateLoss();
        }
    }

    public static boolean isShowing(@Nullable FragmentManager manager) {
        return manager != null && (manager.findFragmentByTag(TabTrayFragment.FRAGMENT_TAG) != null);
    }
}
