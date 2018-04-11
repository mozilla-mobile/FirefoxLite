/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.tabs.tabtray;

import org.mozilla.focus.tabs.Tab;

import java.util.List;

public class TabTrayPresenter implements TabTrayContract.Presenter {

    private TabTrayContract.View view;
    private TabTrayContract.Model model;

    TabTrayPresenter(TabTrayContract.View view, TabTrayContract.Model model) {
        this.view = view;
        this.model = model;
        view.updateData(model.getTabs());
        view.setFocusedTab(model.getCurrentTabPosition());
    }

    @Override
    public void viewReady() {
        List<Tab> tabs = model.getTabs();
        if (tabs.isEmpty()) {
            view.closeTabTray();
        } else {
            view.showFocusedTab(model.getCurrentTabPosition());
        }
    }

    @Override
    public void tabClicked(final int tabPosition) {
        List<Tab> tabs = model.getTabs();
        if (tabPosition < 0 || tabPosition >= tabs.size()) {
            view.closeTabTray();
            return;
        }
        model.switchTab(tabPosition);
        view.tabSwitched(tabPosition);
    }

    @Override
    public void tabCloseClicked(int tabPosition) {
        int oldFocusPos = model.getCurrentTabPosition();

        model.removeTab(tabPosition);

        List<Tab> newTabs = model.getTabs();
        int newFocusTab = model.getCurrentTabPosition();

        view.tabRemoved(tabPosition, oldFocusPos, oldFocusPos, newFocusTab);
        view.updateData(newTabs);

        if (!newTabs.isEmpty()) {
            model.switchTab(newFocusTab);
        } else {
            view.closeTabTray();
            view.navigateToHome();
        }
    }
}
