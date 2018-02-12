/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.tabs;

import android.app.Activity;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import org.mozilla.focus.persistence.TabModel;
import org.mozilla.focus.web.DownloadCallback;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Class to help on tabs management, such as adding or removing tabs.
 */
public class TabsSession {

    /**
     * Context for this session. In current intention, a session belongs to an Activity but not just a Context.
     */
    private Activity activity;

    private List<Tab> tabs = new LinkedList<>();

    private Notifier notifier;

    /**
     * Index to refer a tab which is 'focused' by this session. When this index be changed, session
     * should hoist current tab as well.
     */
    private int currentIdx = -1;

    /**
     * A placeholder to avoid null-checking
     */
    private static DefaultListener sDefaultListener = new DefaultListener();

    private TabsViewListener tabsViewListener = sDefaultListener;
    private TabsChromeListener tabsChromeListener = sDefaultListener;
    private DownloadCallback downloadCallback;

    public TabsSession(@NonNull Activity activity) {
        this.activity = activity;

        this.notifier = new Notifier(activity);
        this.notifier.setChromeListener(sDefaultListener);
    }

    /**
     * To get count of tabs in this session.
     *
     * @return count in integer
     */
    public int getTabsCount() {
        return tabs.size();
    }

    /**
     * Copy reference of tabs which are held by this session.
     *
     * @return new List which is safe to change its order without effect this session
     */
    public List<Tab> getTabs() {
        // create a new list, in case of caller modify this list
        final List<Tab> refs = new ArrayList<>(tabs);
        return refs;
    }

    /**
     * To append tabs from a list of TabModel. If tabs is empty before this call, the first appended
     * tab will be hoisted, otherwise no tab will be hoisted.
     * <p>
     * This is asynchronous call.
     * TODO: make it asynchronous
     *
     * @param models
     */
    public void restoreTabs(List<TabModel> models) {
        for (final TabModel model : models) {
            final Tab tab = new Tab(model);
            bindCallback(tab);
            tabs.add(tab);
        }

        if (tabs.size() > 0 && tabs.size() == models.size()) {
            currentIdx = 0; // first tab
        }
    }

    /**
     * To get data of tabs to store in persistent storage.
     *
     * @return created TabModel of tabs in this session.
     */
    public List<TabModel> getSaveState() {
        final List<TabModel> models = new ArrayList<>();
        for (final Tab tab : tabs) {
            models.add(tab.getSaveModel());
        }
        return models;
    }

    /**
     * Add a tab to tail and create TabView for it, then hoist this new tab.
     *
     * @param url initial url for this tab
     * @return id for created tab
     */
    public String addTab(@Nullable final String url) {
        return addTab(url, true);
    }

    /**
     * Add a tab to tail and create TabView for it.
     *
     * @param url   initial url for this tab
     * @param hoist true to hoist this tab after creation
     * @return id for created tab
     */
    public String addTab(@NonNull final String url, boolean hoist) {
        if (TextUtils.isEmpty(url)) {
            return tabs.get(currentIdx).getId();
        }

        return addTabInternal(url, hoist);
    }

    /**
     * To remove a tab from list.
     *
     * @param id the id of tab to be removed.
     */
    public void removeTab(final String id) {
        final int idx = getTabIndex(id);
        final Tab tab = tabs.get(idx);
        if (tab == null) {
            return;
        }

        tab.destroy();

        // removed one tab, now idx should refer to next one
        currentIdx = idx >= tabs.size() ? tabs.size() - 1 : idx;
        if (hasTabs()) {
            hoistTab(tabs.get(currentIdx));
        }

        tabsChromeListener.onTabCountChanged(tabs.size());
    }

    /**
     * To hoist a tab from list.
     *
     * @param id the id of tab to be hoisted.
     */
    public void switchToTab(final String id) {
        final int idx = getTabIndex(id);
        if (idx < 0 || idx > tabs.size() - 1) {
            return;
        }

        currentIdx = idx;
        hoistTab(tabs.get(currentIdx));
    }

    /**
     * To check whether this session has any tabs
     *
     * @return true if this session has at least one tab
     */
    public boolean hasTabs() {
        return tabs.size() > 0;
    }

    /**
     * To get current focused tab.
     *
     * @return current focused tab. Return null if there is not any tab.
     */
    public Tab getCurrentTab() {
        return (currentIdx >= 0 && currentIdx < tabs.size()) ? tabs.get(currentIdx) : null;
    }

    /**
     * To specify @see{TabsViewListener} to this session, this method will replace existing one.
     *
     * @param listener
     */
    public void setTabsViewListener(@Nullable TabsViewListener listener) {
        tabsViewListener = (listener == null) ? sDefaultListener : listener;
    }

    /**
     * To specify @see{TabsChromeListener} to this session, this method will replace existing one.
     *
     * @param listener
     */
    public void setTabsChromeListener(@Nullable TabsChromeListener listener) {
        tabsChromeListener = (listener == null) ? sDefaultListener : listener;
        this.notifier.setChromeListener(this.tabsChromeListener);
    }

    /**
     * To specify @see{DownloadCallback} to this session, this method will replace existing one. It
     * also replace DownloadCallback from any existing Tab.
     *
     * @param downloadCallback
     */
    public void setDownloadCallback(@Nullable DownloadCallback downloadCallback) {
        this.downloadCallback = downloadCallback;
        if (hasTabs()) {
            for (final Tab tab : tabs) {
                tab.setDownloadCallback(downloadCallback);
            }
        }
    }

    /**
     * To destroy this session, and it also destroy any tabs in this session.
     * This method should be called after any View has been removed from view system.
     * No other methods may be called on this session after destroy.
     */
    public void destroy() {
        for (final Tab tab : tabs) {
            tab.destroy();
        }
    }

    /**
     * To pause this session, and it also pause any tabs in this session.
     */
    public void pause() {
        for (final Tab tab : tabs) {
            tab.pause();
        }
    }

    /**
     * To resume this session after a previous call to @see{#pause}
     */
    public void resume() {
        for (final Tab tab : tabs) {
            tab.resume();
        }
    }

    private void bindCallback(@NonNull Tab tab) {
        tab.setTabViewClient(new TabViewClientImpl(tab));
        tab.setTabChromeClient(new TabChromeClientImpl(tab));
        tab.setDownloadCallback(downloadCallback);
    }

    private String addTabInternal(@Nullable final String url, boolean hoist) {
        final Tab tab = new Tab();

        bindCallback(tab);

        // add to tail
        tabs.add(tab);
        currentIdx = tabs.size() - 1;

        if (!TextUtils.isEmpty(url)) {
            tab.createView(activity).loadUrl(url);
        }

        if (hoist) {
            hoistTab(tab);
        }

        tabsChromeListener.onTabCountChanged(tabs.size());
        return tab.getId();
    }

    private Tab getTab(final @NonNull String id) {
        final int index = getTabIndex(id);
        return index == -1 ? null : tabs.get(index);
    }

    private int getTabIndex(final @NonNull String id) {
        for (int i = 0; i < tabs.size(); i++) {
            final Tab tab = tabs.get(i);
            if (tab.getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private void hoistTab(final Tab tab) {
        final Message msg = notifier.obtainMessage(Notifier.MSG_HOIST_TAB);
        msg.obj = tab;
        notifier.sendMessage(msg);
    }

    class TabViewClientImpl extends TabViewClient {
        @NonNull
        Tab source;

        TabViewClientImpl(@NonNull Tab source) {
            this.source = source;
        }

        @Override
        public void onPageStarted(String url) {
            source.setUrl(url);
            source.setTitle(source.getTabView().getTitle());

            // FIXME: workaround for 'dialog new window'
            if (source.getUrl() != null) {
                tabsViewListener.onTabStarted(source);
            }
        }

        @Override
        public void onPageFinished(boolean isSecure) {
            source.setTitle(source.getTabView().getTitle());

            tabsViewListener.onTabFinished(source, isSecure);
        }

        @Override
        public void onURLChanged(String url) {
            source.setUrl(url);
            source.setTitle(source.getTabView().getTitle());

            tabsViewListener.onURLChanged(source, url);
        }

        @Override
        public boolean handleExternalUrl(String url) {
            return tabsViewListener.handleExternalUrl(url);
        }

        @Override
        public void updateFailingUrl(String url, boolean updateFromError) {
            tabsViewListener.updateFailingUrl(source, url, updateFromError);
        }
    }


    private class TabChromeClientImpl extends TabChromeClient {
        @NonNull
        Tab source;

        TabChromeClientImpl(@NonNull Tab source) {
            this.source = source;
        }

        @Override
        public boolean onCreateWindow(boolean isDialog, boolean isUserGesture, Message msg) {
            if (msg == null) {
                return false;
            }

            final String id = addTabInternal(null, false);
            final Tab tab = getTab(id);
            if (tab == null) {
                // FIXME: why null?
                return false;
            }

            final WebView webView = (WebView) tab.getTabView();
            final WebView.WebViewTransport transport = (WebView.WebViewTransport) msg.obj;
            transport.setWebView(webView);
            msg.sendToTarget();

            tabsChromeListener.onTabHoist(tab);

            return true;
        }

        @Override
        public void onCloseWindow(WebView webView) {
            if (source.getTabView() == webView) {
                for (int i = 0; i < tabs.size(); i++) {
                    final Tab tab = tabs.get(i);
                    if (tab.getTabView() == webView) {
                        removeTab(tab.getId());
                    }
                }
            }
        }

        @Override
        public void onProgressChanged(int progress) {
            tabsChromeListener.onProgressChanged(source, progress);
        }

        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
            return tabsChromeListener.onShowFileChooser(source, webView, filePathCallback, fileChooserParams);
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            tabsViewListener.onReceivedTitle(source, title);
        }

        @Override
        public void onLongPress(TabView.HitTarget hitTarget) {
            tabsChromeListener.onLongPress(source, hitTarget);
        }

        @Override
        public void onEnterFullScreen(@NonNull TabView.FullscreenCallback callback, @Nullable View view) {
            tabsChromeListener.onEnterFullScreen(source, callback);
        }

        @Override
        public void onExitFullScreen() {
            tabsChromeListener.onExitFullScreen(source);
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            tabsChromeListener.onGeolocationPermissionsShowPrompt(source, origin, callback);
        }
    }

    /**
     * A class to attach to UI thread for sending message.
     */
    private static class Notifier extends Handler {
        static final int MSG_HOIST_TAB = 0x1001;

        private Activity activity;
        private TabsChromeListener chromeListener;

        Notifier(@NonNull final Activity activity) {
            super(Looper.getMainLooper());
            this.activity = activity;
        }

        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case MSG_HOIST_TAB:
                    hoistTab((Tab) msg.obj);
                    break;
                default:
                    break;
            }
        }

        void setChromeListener(TabsChromeListener listener) {
            this.chromeListener = listener;
        }

        private void hoistTab(final Tab tab) {
            if (tab != null && tab.getTabView() == null) {
                tab.createView(this.activity);
            }

            this.chromeListener.onTabHoist(tab);
        }
    }

    /**
     * A empty implementation to avoid null-checking.
     */
    private static class DefaultListener implements TabsViewListener, TabsChromeListener {

        @Override
        public void onTabStarted(@NonNull Tab tab) {
        }

        @Override
        public void onTabFinished(@NonNull Tab tab, boolean isSecure) {

        }

        @Override
        public void onURLChanged(@NonNull Tab tab, String url) {
        }

        @Override
        public boolean handleExternalUrl(String url) {
            return false;
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(@NonNull Tab tabView, String origin, GeolocationPermissions.Callback callback) {
        }

        @Override
        public void updateFailingUrl(@NonNull Tab tab, String url, boolean updateFromError) {
        }

        @Override
        public void onReceivedTitle(@NonNull Tab tab, String title) {
        }

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
        public void onEnterFullScreen(@NonNull Tab tab, @NonNull TabView.FullscreenCallback callback) {
        }

        @Override
        public void onExitFullScreen(@NonNull Tab tab) {
        }

        @Override
        public boolean onShowFileChooser(@NonNull Tab tab,
                                         WebView webView,
                                         ValueCallback<Uri[]> filePathCallback,
                                         WebChromeClient.FileChooserParams fileChooserParams) {
            return false;
        }
    }
}
