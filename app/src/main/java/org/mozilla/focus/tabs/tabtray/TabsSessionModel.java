/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.tabs.tabtray;

import android.support.annotation.NonNull;

import org.mozilla.focus.BuildConfig;
import org.mozilla.focus.tabs.Tab;
import org.mozilla.focus.tabs.TabsSession;

import java.util.List;

class TabsSessionModel implements TabTrayContract.Model {
    @NonNull
    private TabsSession tabsSession;

    TabsSessionModel(@NonNull TabsSession tabsSession) {
        this.tabsSession = tabsSession;
    }

    @Override
    public List<Tab> getTabs() {
        return tabsSession.getTabs();
    }

    @Override
    public int getCurrentTabPosition() {
        return tabsSession.getTabs().indexOf(tabsSession.getFocusTab());
    }

    @Override
    public void switchTab(int tabPosition) {
        final List<Tab> tabs = tabsSession.getTabs();
        if (tabPosition < 0 || tabPosition >= tabs.size()) {
            if (BuildConfig.DEBUG) {
                throw new ArrayIndexOutOfBoundsException("index: " + tabPosition + ", size: " + tabs.size());
            }
            return;
        }

        tabsSession.switchToTab(tabs.get(tabPosition).getId());
    }

    @Override
    public void removeTab(int tabPosition) {
        final List<Tab> tabs = tabsSession.getTabs();
        if (tabPosition < 0 || tabPosition >= tabs.size()) {
            if (BuildConfig.DEBUG) {
                throw new ArrayIndexOutOfBoundsException("index: " + tabPosition + ", size: " + tabs.size());
            }
            return;
        }

        tabsSession.dropTab(tabs.get(tabPosition).getId());
    }
}
