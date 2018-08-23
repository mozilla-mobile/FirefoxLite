/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.tabs.utils;

import android.support.annotation.NonNull;

import org.mozilla.rocket.tabs.Session;
import org.mozilla.rocket.tabs.TabsViewListener;

/**
 * A sugar class implements TabsViewListener in empty implementation.
 */
public class DefaultTabsViewListener implements TabsViewListener {
    @Override
    public void onTabStarted(@NonNull Session tab) {
    }

    @Override
    public void onTabFinished(@NonNull Session tab, boolean isSecure) {
    }

    @Override
    public void onURLChanged(@NonNull Session tab, String url) {
    }

    @Override
    public boolean handleExternalUrl(String url) {
        return false;
    }

    @Override
    public void updateFailingUrl(@NonNull Session tab, String url, boolean updateFromError) {
    }
}
