/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget;

public class TabTrayPresenter implements TabTrayContract.Presenter {

    private TabTrayContract.View view;
    private TabTrayContract.Model model;

    TabTrayPresenter(TabTrayContract.View view, TabTrayContract.Model model) {
        this.view = view;
        this.model = model;
        view.showTabs(model.getTabs());
    }

    @Override
    public void viewReady() {
        view.setFocusedTab(model.getCurrentTabPosition());
        view.showFocusedTab(model.getCurrentTabPosition());
    }

    @Override
    public void tabClicked(int tabPosition) {
        model.switchTab(tabPosition);
        view.tabSwitched(tabPosition);
    }

    @Override
    public void tabCloseClicked(int tabPosition) {
        int oldFocusTab = model.getCurrentTabPosition();
        model.removeTab(tabPosition);
        view.tabRemoved(tabPosition, oldFocusTab, model.getCurrentTabPosition());
    }
}
