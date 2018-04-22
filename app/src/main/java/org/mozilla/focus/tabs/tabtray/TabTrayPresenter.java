/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.tabs.tabtray;

import org.mozilla.focus.tabs.Tab;

import java.util.List;

public class TabTrayPresenter implements TabTrayContract.Presenter {

    private final TabTrayContract.View view;
    private final TabTrayContract.Model model;

    TabTrayPresenter(final TabTrayContract.View view, final TabTrayContract.Model model) {
        this.view = view;
        this.model = model;
        this.model.loadTabs(new TabTrayContract.Model.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete() {
                view.initData(model.getTabs(), model.getFocusedTab());
            }
        });
    }

    @Override
    public void viewReady() {
        final List<Tab> tabs = model.getTabs();
        if (tabs.isEmpty()) {
            view.closeTabTray();
        } else {
            model.subscribe(new TabTrayContract.Model.Observer() {
                @Override
                public void onUpdate(List<Tab> newTabs) {
                    view.refreshData(newTabs, model.getFocusedTab());
                    model.loadTabs(null);
                }

                @Override
                public void onTabUpdate(Tab tab) {
                    view.refreshTabData(tab);
                }
            });
            view.showFocusedTab(tabs.indexOf(model.getFocusedTab()));
        }
    }

    @Override
    public void tabClicked(final int tabPosition) {
        model.switchTab(tabPosition);
        view.tabSwitched(tabPosition);
    }

    @Override
    public void tabCloseClicked(int tabPosition) {
        model.removeTab(tabPosition);

        List<Tab> newTabs = model.getTabs();
        int newFocusTab = newTabs.indexOf(model.getFocusedTab());

        if (newTabs.isEmpty()) {
            view.closeTabTray();
            view.navigateToHome();
        } else if (newFocusTab >= 0 && newFocusTab < newTabs.size()) {
            model.switchTab(newFocusTab);
        }
    }

    @Override
    public void tabTrayClosed() {
        model.unsubscribe();
    }
}
