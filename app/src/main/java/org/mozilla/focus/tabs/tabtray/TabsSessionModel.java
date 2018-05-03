/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.tabs.tabtray;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;

import org.mozilla.focus.BuildConfig;
import org.mozilla.focus.tabs.Tab;
import org.mozilla.focus.tabs.TabView;
import org.mozilla.focus.tabs.TabsChromeListener;
import org.mozilla.focus.tabs.TabsSession;
import org.mozilla.focus.tabs.TabsViewListener;

import java.util.ArrayList;
import java.util.List;

class TabsSessionModel implements TabTrayContract.Model {
    @NonNull
    private TabsSession tabsSession;
    private OnTabModelChangedListener onTabModelChangedListener;

    private List<Tab> tabs = new ArrayList<>();

    TabsSessionModel(@NonNull TabsSession tabsSession) {
        this.tabsSession = tabsSession;
    }

    @Override
    public void loadTabs(OnLoadCompleteListener listener) {
        tabs.clear();
        tabs.addAll(tabsSession.getTabs());

        if (listener != null) {
            listener.onLoadComplete();
        }
    }

    @Override
    public List<Tab> getTabs() {
        return tabs;
    }

    @Override
    public Tab getFocusedTab() {
        return tabsSession.getFocusTab();
    }

    @Override
    public void switchTab(int tabPosition) {
        if (tabPosition >= 0 && tabPosition < tabs.size()) {
            Tab target = tabs.get(tabPosition);
            List<Tab> latestTabs = tabsSession.getTabs();
            boolean exist = latestTabs.indexOf(target) != -1;
            if (exist) {
                tabsSession.switchToTab(target.getId());
            }
        } else {
            if (BuildConfig.DEBUG) {
                throw new ArrayIndexOutOfBoundsException("index: " + tabPosition + ", size: " + tabs.size());
            }
        }
    }

    @Override
    public void removeTab(int tabPosition) {
        if (tabPosition >= 0 && tabPosition < tabs.size()) {
            tabsSession.dropTab(tabs.get(tabPosition).getId());
        } else {
            if (BuildConfig.DEBUG) {
                throw new ArrayIndexOutOfBoundsException("index: " + tabPosition + ", size: " + tabs.size());
            }
        }
    }

    @Override
    public void clearTabs() {
        List<Tab> tabs = tabsSession.getTabs();
        for (Tab tab : tabs) {
            tabsSession.dropTab(tab.getId());
        }
    }

    @Override
    public void subscribe(final Observer observer) {
        if (onTabModelChangedListener == null) {
            onTabModelChangedListener = new OnTabModelChangedListener() {
                @Override
                void onTabModelChanged(Tab tab) {
                    observer.onTabUpdate(tab);
                }

                @Override
                public void onTabCountChanged(int count) {
                    observer.onUpdate(tabsSession.getTabs());
                }
            };
        }
        tabsSession.addTabsViewListener(onTabModelChangedListener);
        tabsSession.addTabsChromeListener(onTabModelChangedListener);
    }

    @Override
    public void unsubscribe() {
        if (onTabModelChangedListener != null) {
            tabsSession.removeTabsViewListener(onTabModelChangedListener);
            tabsSession.removeTabsChromeListener(onTabModelChangedListener);
            onTabModelChangedListener = null;
        }
    }

    private static abstract class OnTabModelChangedListener implements TabsViewListener,
            TabsChromeListener {
        @Override
        public void onTabStarted(@NonNull Tab tab) {
        }

        @Override
        public void onTabFinished(@NonNull Tab tab, boolean isSecure) {
        }

        @Override
        public void onURLChanged(@NonNull Tab tab, String url) {
            onTabModelChanged(tab);
        }

        @Override
        public boolean handleExternalUrl(String url) {
            return false;
        }

        @Override
        public void updateFailingUrl(@NonNull Tab tab, String url, boolean updateFromError) {
            onTabModelChanged(tab);
        }

        @Override
        public void onProgressChanged(@NonNull Tab tab, int progress) {
        }

        @Override
        public void onReceivedTitle(@NonNull Tab tab, String title) {
            onTabModelChanged(tab);
        }

        @Override
        public void onReceivedIcon(@NonNull Tab tab, Bitmap icon) {
            onTabModelChanged(tab);
        }

        @Override
        public void onFocusChanged(@Nullable Tab tab, @Factor int factor) {

        }

        @Override
        public void onTabAdded(@NonNull Tab tab, @Nullable Bundle arguments) {
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
        public boolean onShowFileChooser(@NonNull Tab tab, TabView tabView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
            return false;
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(@NonNull Tab tab, String origin, GeolocationPermissions.Callback callback) {

        }

        abstract void onTabModelChanged(Tab tab);
    }
}
