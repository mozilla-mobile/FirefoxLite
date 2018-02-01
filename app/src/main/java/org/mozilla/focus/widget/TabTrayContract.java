/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.mozilla.focus.widget;

import org.mozilla.focus.tabs.Tab;

import java.util.List;

class TabTrayContract {
    interface Presenter {
        void tabClicked(Tab tab);
        void tabCloseClicked(Tab tab);
    }

    interface View {
        void tabSwitched(int tabPosition);
        void tabRemoved(int tabPosition);
    }

    interface Model {
        List<Tab> getTabs();

        int getTabCount();
        int getCurrentTabPosition();

        void switchTab(int tabIdx);
        void removeTab(Tab tab);
    }
}
