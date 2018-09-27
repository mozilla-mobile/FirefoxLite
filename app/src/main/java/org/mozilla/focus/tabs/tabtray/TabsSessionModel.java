/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.tabs.tabtray;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;

import org.mozilla.focus.BuildConfig;
import org.mozilla.rocket.tabs.Session;
import org.mozilla.rocket.tabs.SessionManager;
import org.mozilla.rocket.tabs.TabView;
import org.mozilla.rocket.tabs.TabsChromeListener;
import org.mozilla.rocket.tabs.TabsViewListener;

import java.util.ArrayList;
import java.util.List;

class TabsSessionModel implements TabTrayContract.Model {
    @NonNull
    private SessionManager sessionManager;
    private OnTabModelChangedListener onTabModelChangedListener;

    private List<Session> tabs = new ArrayList<>();

    TabsSessionModel(@NonNull SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void loadTabs(OnLoadCompleteListener listener) {
        tabs.clear();
        tabs.addAll(sessionManager.getTabs());

        if (listener != null) {
            listener.onLoadComplete();
        }
    }

    @Override
    public List<Session> getTabs() {
        return tabs;
    }

    @Override
    public Session getFocusedTab() {
        return sessionManager.getFocusSession();
    }

    @Override
    public void switchTab(int tabPosition) {
        if (tabPosition >= 0 && tabPosition < tabs.size()) {
            Session target = tabs.get(tabPosition);
            List<Session> latestTabs = sessionManager.getTabs();
            boolean exist = latestTabs.indexOf(target) != -1;
            if (exist) {
                sessionManager.switchToTab(target.getId());
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
            sessionManager.dropTab(tabs.get(tabPosition).getId());
        } else {
            if (BuildConfig.DEBUG) {
                throw new ArrayIndexOutOfBoundsException("index: " + tabPosition + ", size: " + tabs.size());
            }
        }
    }

    @Override
    public void clearTabs() {
        List<Session> tabs = sessionManager.getTabs();
        for (Session tab : tabs) {
            sessionManager.dropTab(tab.getId());
        }
    }

    @Override
    public void subscribe(final Observer observer) {
        if (onTabModelChangedListener == null) {
            onTabModelChangedListener = new OnTabModelChangedListener() {
                @Override
                void onTabModelChanged(Session tab) {
                    observer.onTabUpdate(tab);
                }

                @Override
                public void onTabCountChanged(int count) {
                    observer.onUpdate(sessionManager.getTabs());
                }
            };
        }
        sessionManager.addTabsViewListener(onTabModelChangedListener);
        sessionManager.addTabsChromeListener(onTabModelChangedListener);
    }

    @Override
    public void unsubscribe() {
        if (onTabModelChangedListener != null) {
            sessionManager.removeTabsViewListener(onTabModelChangedListener);
            sessionManager.removeTabsChromeListener(onTabModelChangedListener);
            onTabModelChangedListener = null;
        }
    }

    private static abstract class OnTabModelChangedListener implements TabsViewListener,
            TabsChromeListener {
        @Override
        public void onTabStarted(@NonNull Session tab) {
        }

        @Override
        public void onTabFinished(@NonNull Session tab, boolean isSecure) {
        }

        @Override
        public void onURLChanged(@NonNull Session tab, String url) {
            onTabModelChanged(tab);
        }

        @Override
        public boolean handleExternalUrl(String url) {
            return false;
        }

        @Override
        public void updateFailingUrl(@NonNull Session tab, String url, boolean updateFromError) {
            onTabModelChanged(tab);
        }

        @Override
        public void onProgressChanged(@NonNull Session tab, int progress) {
        }

        @Override
        public void onReceivedTitle(@NonNull Session tab, String title) {
            onTabModelChanged(tab);
        }

        @Override
        public void onReceivedIcon(@NonNull Session tab, Bitmap icon) {
            onTabModelChanged(tab);
        }

        @Override
        public void onFocusChanged(@Nullable Session tab, @Factor int factor) {

        }

        @Override
        public void onTabAdded(@NonNull Session tab, @Nullable Bundle arguments) {
        }

        @Override
        public void onTabCountChanged(int count) {
        }

        @Override
        public void onLongPress(@NonNull Session tab, TabView.HitTarget hitTarget) {
        }

        @Override
        public void onEnterFullScreen(@NonNull Session tab, @NonNull TabView.FullscreenCallback callback, @Nullable View fullscreenContent) {
        }

        @Override
        public void onExitFullScreen(@NonNull Session tab) {
        }

        @Override
        public boolean onShowFileChooser(@NonNull Session tab, TabView tabView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
            return false;
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(@NonNull Session tab, String origin, GeolocationPermissions.Callback callback) {

        }

        abstract void onTabModelChanged(Session tab);
    }
}
