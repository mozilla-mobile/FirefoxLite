/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.tabs.tabtray;


import java.util.ArrayList;
import java.util.List;

import mozilla.components.browser.session.Session;

public class TabTrayPresenter implements TabTrayContract.Presenter {

    private TabTrayContract.View view;
    private TabTrayContract.Model model;

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
        final List<Session> tabs = model.getTabs();
        if (tabs.isEmpty()) {
            view.closeTabTray();
        } else {
            model.subscribe(new TabTrayContract.Model.Observer() {
                @Override
                public void onUpdate(List<Session> newTabs) {
                    view.refreshData(newTabs, model.getFocusedTab());
                    model.loadTabs(null);
                }

                @Override
                public void onTabUpdate(Session tab) {
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

        List<Session> newTabs = model.getTabs();
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

    @Override
    public void closeAllTabs() {
        view.refreshData(new ArrayList<Session>(), null);
        view.closeTabTray();
        view.navigateToHome();

        model.clearTabs();
    }
}
