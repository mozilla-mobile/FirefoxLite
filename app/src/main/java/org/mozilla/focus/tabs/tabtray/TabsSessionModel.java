/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.tabs.tabtray;

import android.app.Activity;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import org.mozilla.focus.BuildConfig;
import org.mozilla.focus.tabs.Tab;
import org.mozilla.focus.tabs.TabView;
import org.mozilla.focus.tabs.TabsChromeListener;
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
    public void switchTab(int tabIdx, final Runnable finishCallback) {
        final List<Tab> tabs = tabsSession.getTabs();
        if (tabIdx < 0 || tabIdx >= tabs.size()) {
            if (BuildConfig.DEBUG) {
                throw new ArrayIndexOutOfBoundsException("index: " + tabIdx + ", size: " + tabs.size());
            }
            return;
        }

        // TODO: Any better approach?
        // TabsSession#switchToTab() will implicitly post a call to TabsChromeListener#onTabHoist().
        // By monitoring onTabHoist(), we can make sure finishCallback is run in the same frame loop
        // with the others onTabHoist()
        tabsSession.addTabsChromeListener(new TabsChromeAdapter() {
            @Override
            public void onTabHoist(@NonNull Tab tab) {
                tabsSession.removeTabsChromeListener(this);
                finishCallback.run();
            }
        });
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

    private static class TabsChromeAdapter implements TabsChromeListener {

        @Override
        public void onProgressChanged(@NonNull Tab tab, int progress) {
        }

        @Override
        public void onTabHoist(@NonNull Tab tab) {
        }

        @Override
        public void onTabCountChanged(int count) {
        }

        @Override
        public void onLongPress(@NonNull Tab tab, TabView.HitTarget hitTarget) {
        }

        @Override
        public void onEnterFullScreen(@NonNull Tab tab, @NonNull TabView.FullscreenCallback callback, @Nullable View fullscreenContent) {
        }

        @Override
        public void onExitFullScreen(@NonNull Tab tab) {
        }

        @Override
        public boolean onShowFileChooser(@NonNull Tab tab, WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
            return false;
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(@NonNull Tab tab, String origin, GeolocationPermissions.Callback callback) {
        }
    }
}
