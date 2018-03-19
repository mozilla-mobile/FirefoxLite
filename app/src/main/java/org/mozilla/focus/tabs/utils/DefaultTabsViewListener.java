/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.tabs.utils;

import android.support.annotation.NonNull;

import org.mozilla.focus.tabs.Tab;
import org.mozilla.focus.tabs.TabsViewListener;

/**
 * A sugar class implements TabsViewListener in empty implementation.
 */
public class DefaultTabsViewListener implements TabsViewListener {
    @Override
    public void onTabStarted(@NonNull Tab tab) {

    }

    @Override
    public void onTabFinished(@NonNull Tab tab, boolean isSecure) {

    }

    @Override
    public void onURLChanged(@NonNull Tab tab, String url) {

    }

    @Override
    public boolean handleExternalUrl(String url) {
        return false;
    }

    @Override
    public void updateFailingUrl(@NonNull Tab tab, String url, boolean updateFromError) {

    }
}
