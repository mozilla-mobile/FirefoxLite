/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.tabs.tabtray;

import org.mozilla.focus.tabs.Tab;

import java.util.List;

class TabTrayContract {
    interface Presenter {
        void viewReady();
        void tabClicked(int tabPosition);
        void tabCloseClicked(int tabPosition);
    }

    interface View {
        void updateData(List<Tab> tabs);
        void setFocusedTab(int tabPosition);
        void showFocusedTab(int tabPosition);
        void tabSwitched(int tabPosition);
        void tabRemoved(int removePos, int focusPos, int modifiedFocusPos, int nextFocusPos);
        void closeTabTray();
        void navigateToHome();
    }

    interface Model {
        List<Tab> getTabs();

        int getCurrentTabPosition();

        void switchTab(int tabPosition);
        void removeTab(int tabPosition);
    }
}
