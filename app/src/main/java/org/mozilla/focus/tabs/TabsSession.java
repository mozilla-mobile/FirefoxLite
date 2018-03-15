/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.tabs;

import android.app.Activity;
import android.graphics.Bitmap;
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

import java.lang.ref.WeakReference;
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

    private WeakReference<Tab> focusRef = new WeakReference<>(null);

    private List<TabsViewListener> tabsViewListeners = new ArrayList<>();
    private List<TabsChromeListener> tabsChromeListeners = new ArrayList<>();
    private DownloadCallback downloadCallback;

    public TabsSession(@NonNull Activity activity) {
        this.activity = activity;

        this.notifier = new Notifier(activity, this.tabsChromeListeners);
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
    public void restoreTabs(@NonNull final List<TabModel> models, String focusTabId) {
        for (final TabModel model : models) {
            if (!TabModel.isSane(model)) {
                continue;
            }

            final Tab tab = new Tab(model);
            bindCallback(tab);
            tabs.add(tab);
        }

        if (tabs.size() > 0 && tabs.size() == models.size()) {
            focusRef = new WeakReference<>(getTab(focusTabId));
        }

        for (final TabsChromeListener l : tabsChromeListeners) {
            l.onTabCountChanged(tabs.size());
        }
    }

    /**
     * To get data of tabs to store in persistent storage.
     *
     * @return created TabModel of tabs in this session.
     */
    public List<TabModel> getTabModelListForPersistence() {
        final List<TabModel> models = new ArrayList<>();
        for (final Tab tab : tabs) {
            models.add(tab.getSaveModel());
        }
        return models;
    }

    /**
     * Add a tab to a specific parent tab.
     *
     * @param parentId     id of parent tab. If it is null, the tab will be append to tail
     * @param url          initial url for this tab
     * @param fromExternal is this request from external app
     * @param hoist        true to hoist this tab after creation
     * @return id for created tab
     */
    @Nullable
    public String addTab(@Nullable final String parentId,
                         @NonNull final String url,
                         boolean fromExternal,
                         boolean hoist) {

        if (TextUtils.isEmpty(url)) {
            return null;
        }

        return addTabInternal(parentId, url, fromExternal, hoist);
    }

    /**
     * To drop a tab from list, it will not invoke callback onTabHoist, and only change focus to nearest tab.
     *
     * @param id the id of tab to be dropped
     */
    public void dropTab(@NonNull final String id) {
        this.removeTabInternal(id, true);
    }

    /**
     * To close a tab by remove it from list and update tab focus.
     *
     * @param id the id of tab to be closed.
     */
    public void closeTab(@NonNull final String id) {
        this.removeTabInternal(id, false);
    }

    private void removeTabInternal(final String id, final boolean isDrop) {
        final Tab tab = getTab(id);
        if (tab == null) {
            return;
        }

        final int oldIndex = getTabIndex(id);
        tabs.remove(tab);
        tab.destroy();

        // Update child's parent id to its ancestor
        // TODO: in our current design, the parent of a tab are always locate at left(index -1).
        // hence no need to loop whole list.
        for (final Tab t : tabs) {
            if (TextUtils.equals(t.getParentId(), tab.getId())) {
                t.setParentId(tab.getParentId());
            }
        }

        // if the removing tab was focused, we need to update focus
        if (tab == focusRef.get()) {
            if (isDrop) {
                final int nextIdx = Math.min(oldIndex, tabs.size() - 1);
                focusRef = (nextIdx == -1)
                        ? new WeakReference<Tab>(null)
                        : new WeakReference<>(tabs.get(nextIdx));
            } else {
                updateFocusOnClosing(tab);
            }
        }

        for (final TabsChromeListener l : tabsChromeListeners) {
            l.onTabCountChanged(tabs.size());
        }
    }

    private void updateFocusOnClosing(final Tab removedTab) {
        if (TextUtils.isEmpty(removedTab.getParentId())) {
            focusRef.clear();
            notifyTabHoisted(null, TabsChromeListener.FACTOR_NO_FOCUS);
        } else if (TextUtils.equals(removedTab.getParentId(), Tab.ID_EXTERNAL)) {
            focusRef.clear();
            notifyTabHoisted(null, TabsChromeListener.FACTOR_BACK_EXTERNAL);
        } else {
            focusRef = new WeakReference<>(getTab(removedTab.getParentId()));
            notifyTabHoisted(focusRef.get(), TabsChromeListener.FACTOR_TAB_REMOVED);
        }
    }

    /**
     * To hoist a tab from list.
     *
     * @param id the id of tab to be hoisted.
     */
    public void switchToTab(final String id) {
        final Tab nextTab = getTab(id);
        if (nextTab != null) {
            focusRef = new WeakReference<>(nextTab);
        }

        notifyTabHoisted(nextTab, TabsChromeListener.FACTOR_TAB_SWITCHED);
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
    @Nullable
    public Tab getFocusTab() {
        return focusRef.get();
    }

    /**
     * To add @see{TabsViewListener} to this session.
     *
     * @param listener
     */
    public void addTabsViewListener(@NonNull TabsViewListener listener) {
        if (!this.tabsViewListeners.contains(listener)) {
            this.tabsViewListeners.add(listener);
        }
    }

    /**
     * To add @see{TabsChromeListener} to this session.
     *
     * @param listener
     */
    public void addTabsChromeListener(@Nullable TabsChromeListener listener) {
        if (!this.tabsChromeListeners.contains(listener)) {
            this.tabsChromeListeners.add(listener);
        }
    }

    /**
     * To remove @see{TabsViewListener} from this session.
     *
     * @param listener
     */
    public void removeTabsViewListener(@NonNull TabsViewListener listener) {
        this.tabsViewListeners.remove(listener);
    }

    /**
     * To remove @see{TabsChromeListener} from this session.
     *
     * @param listener
     */
    public void removeTabsChromeListener(@NonNull TabsChromeListener listener) {
        this.tabsChromeListeners.remove(listener);
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

    private String addTabInternal(@Nullable final String parentId,
                                  @Nullable final String url,
                                  boolean fromExternal,
                                  boolean hoist) {

        final Tab tab = new Tab();
        tab.setUrl(url);

        bindCallback(tab);

        final int parentIndex = (TextUtils.isEmpty(parentId)) ? -1 : getTabIndex(parentId);
        if (fromExternal) {
            tab.setParentId(Tab.ID_EXTERNAL);
            tabs.add(tab);
        } else {
            insertTab(parentIndex, tab);
        }

        focusRef = (hoist || fromExternal) ? new WeakReference<>(tab) : focusRef;

        if (!TextUtils.isEmpty(url)) {
            tab.createView(activity).loadUrl(url);
        }

        if (hoist || fromExternal) {
            notifyTabHoisted(tab, TabsChromeListener.FACTOR_TAB_ADDED);
        }

        for (final TabsChromeListener l : tabsChromeListeners) {
            l.onTabCountChanged(tabs.size());
        }
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

    private void insertTab(final int parentIdx, @NonNull final Tab tab) {
        final Tab parentTab = (parentIdx >= 0 && parentIdx < tabs.size())
                ? tabs.get(parentIdx)
                : null;
        if (parentTab == null) {
            tabs.add(tab);
            return;
        } else {
            tabs.add(parentIdx + 1, tab);
        }

        // TODO: in our current design, the parent of a tab are always locate at left(index -1).
        //       hence no need to loop whole list.
        // if the parent-tab has a child, give it a new parent
        for (final Tab t : tabs) {
            if (parentTab.getId().equals(t.getParentId())) {
                t.setParentId(tab.getId());
            }
        }

        // update family relationship
        tab.setParentId(parentTab.getId());
    }

    private void notifyTabHoisted(final Tab tab, final @TabsChromeListener.Factor int factor) {
        final Message msg = notifier.obtainMessage(Notifier.MSG_HOIST_TAB);
        msg.obj = tab;
        msg.arg1 = factor;
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
                for (final TabsViewListener l : tabsViewListeners) {
                    l.onTabStarted(source);
                }
            }
        }

        @Override
        public void onPageFinished(boolean isSecure) {
            source.setTitle(source.getTabView().getTitle());

            for (final TabsViewListener l : tabsViewListeners) {
                l.onTabFinished(source, isSecure);
            }
        }

        @Override
        public void onURLChanged(String url) {
            source.setUrl(url);
            source.setTitle(source.getTabView().getTitle());

            for (final TabsViewListener l : tabsViewListeners) {
                l.onURLChanged(source, url);
            }
        }

        @Override
        public boolean handleExternalUrl(String url) {
            // only return false if none of listeners handled external url.
            for (final TabsViewListener l : tabsViewListeners) {
                if (l.handleExternalUrl(url)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void updateFailingUrl(String url, boolean updateFromError) {
            for (final TabsViewListener l : tabsViewListeners) {
                l.updateFailingUrl(source, url, updateFromError);
            }
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

            final String id = addTabInternal(source.getId(), null, false, false);
            final Tab tab = getTab(id);
            if (tab == null) {
                // FIXME: why null?
                return false;
            }

            final WebView webView = (WebView) tab.getTabView();
            final WebView.WebViewTransport transport = (WebView.WebViewTransport) msg.obj;
            transport.setWebView(webView);
            msg.sendToTarget();

            notifyTabHoisted(tab, TabsChromeListener.FACTOR_TAB_ADDED);
            return true;
        }

        @Override
        public void onCloseWindow(WebView webView) {
            if (source.getTabView() == webView) {
                for (int i = 0; i < tabs.size(); i++) {
                    final Tab tab = tabs.get(i);
                    if (tab.getTabView() == webView) {
                        closeTab(tab.getId());
                    }
                }
            }
        }

        @Override
        public void onProgressChanged(int progress) {
            for (final TabsChromeListener l : tabsChromeListeners) {
                l.onProgressChanged(source, progress);
            }
        }

        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
            for (final TabsChromeListener l : tabsChromeListeners) {
                if (l.onShowFileChooser(source, webView, filePathCallback, fileChooserParams)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            for (final TabsChromeListener l : tabsChromeListeners) {
                l.onReceivedTitle(source, title);
            }
        }

        @Override
        public void onReceivedIcon(WebView view, Bitmap icon) {
            source.setFavicon(icon);
            for (final TabsChromeListener l : tabsChromeListeners) {
                l.onReceivedIcon(source, icon);
            }
        }

        @Override
        public void onLongPress(TabView.HitTarget hitTarget) {
            for (final TabsChromeListener l : tabsChromeListeners) {
                l.onLongPress(source, hitTarget);
            }
        }

        @Override
        public void onEnterFullScreen(@NonNull TabView.FullscreenCallback callback, @Nullable View view) {
            for (final TabsChromeListener l : tabsChromeListeners) {
                l.onEnterFullScreen(source, callback, view);
            }
        }

        @Override
        public void onExitFullScreen() {
            for (final TabsChromeListener l : tabsChromeListeners) {
                l.onExitFullScreen(source);
            }
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            for (final TabsChromeListener l : tabsChromeListeners) {
                l.onGeolocationPermissionsShowPrompt(source, origin, callback);
            }
        }
    }

    /**
     * A class to attach to UI thread for sending message.
     */
    private static class Notifier extends Handler {
        static final int MSG_HOIST_TAB = 0x1001;

        private Activity activity;
        private List<TabsChromeListener> chromeListeners = null;

        Notifier(@NonNull final Activity activity,
                 @NonNull final List<TabsChromeListener> listeners) {

            super(Looper.getMainLooper());
            this.activity = activity;
            this.chromeListeners = listeners;
        }

        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case MSG_HOIST_TAB:
                    hoistTab((Tab) msg.obj, msg.arg1);
                    break;
                default:
                    break;
            }
        }

        private void hoistTab(final Tab tab, @TabsChromeListener.Factor final int factor) {

            if (tab != null && tab.getTabView() == null) {
                String url = tab.getUrl();
                tab.createView(this.activity).loadUrl(url);
            }

            for (final TabsChromeListener l : this.chromeListeners) {
                l.onTabHoist(tab, factor);
            }
        }
    }
}
