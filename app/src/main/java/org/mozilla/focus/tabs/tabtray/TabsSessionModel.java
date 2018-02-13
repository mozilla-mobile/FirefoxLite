/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.tabs.tabtray;

import android.app.Activity;
import android.support.annotation.NonNull;

import org.mozilla.focus.tabs.Tab;
import org.mozilla.focus.tabs.TabsSession;
import org.mozilla.focus.tabs.TabsSessionProvider;

import java.util.List;

class TabsSessionModel implements TabTrayContract.Model {
    @NonNull
    private TabsSession tabsSession;

    TabsSessionModel(TabTrayFragment fragment) {
        tabsSession = locateTabsSession(fragment);
    }

    @Override
    public List<Tab> getTabs() {
        return tabsSession.getTabs();
    }

    @Override
    public int getCurrentTabPosition() {
        return tabsSession.getTabs().indexOf(tabsSession.getCurrentTab());
    }

    @Override
    public void switchTab(int tabIdx) {
        final List<Tab> tabs = tabsSession.getTabs();
        tabsSession.switchToTab(tabs.get(tabIdx).getId());
    }

    @Override
    public void removeTab(int tabPosition) {
        final List<Tab> tabs = tabsSession.getTabs();
        tabsSession.removeTab(tabs.get(tabPosition).getId());
    }

    @NonNull
    private TabsSession locateTabsSession(TabTrayFragment fragment) {
        Activity activity = fragment.getActivity();
        return TabsSessionProvider.getOrThrow(activity);
    }
}
