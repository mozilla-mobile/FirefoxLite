/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.tabs;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import org.mozilla.focus.persistence.TabModel;
import org.mozilla.focus.web.DownloadCallback;
import org.mozilla.focus.web.WebViewProvider;

import java.util.UUID;

public class Tab {

    public static final String ID_EXTERNAL = "_open_from_external_";

    private static final String TAG = "Tab";

    /**
     * A placeholder in case of there is no callback to use.
     */
    private static TabViewClient sDefViewClient = new TabViewClient();
    private static TabChromeClient sDefChromeClient = new TabChromeClient();

    private TabModel tabModel;
    private TabView tabView;
    private TabViewClient tabViewClient = sDefViewClient;
    private TabChromeClient tabChromeClient = sDefChromeClient;
    private DownloadCallback downloadCallback;

    public Tab() {
        this(new TabModel(UUID.randomUUID().toString(), ""));
    }

    public Tab(@NonNull TabModel model) {
        this.tabModel = model;
    }

    public TabModel getSaveModel() {
        if (tabModel.getWebViewState() == null) {
            tabModel.setWebViewState(new Bundle());
        }

        if (tabView != null) {
            tabModel.setTitle(tabView.getTitle());
            tabModel.setUrl(tabView.getUrl());
            tabView.saveViewState(tabModel.getWebViewState());
        }

        return tabModel;
    }

    public TabView getTabView() {
        return tabView;
    }

    public String getId() {
        return this.tabModel.getId();
    }

    public String getTitle() {
        if (tabView == null) {
            return tabModel.getTitle();
        } else {
            return tabView.getTitle();
        }
    }

    public String getUrl() {
        if (tabView == null) {
            if (tabModel == null) {
                Log.d(TAG, "trying to get url from a tab which has no view nor model");
                return null;
            } else {
                return tabModel.getUrl();
            }
        } else {
            return tabView.getUrl();
        }
    }

    @Nullable
    public Bitmap getFavicon() {
        if (tabModel != null) {
            return tabModel.getFavicon();
        }
        return null;
    }

    @SiteIdentity.SecurityState
    public int getSecurityState() {
        if (tabView == null) {
            return SiteIdentity.UNKNOWN;
        }
        return tabView.getSecurityState();
    }

    void setTabViewClient(@Nullable final TabViewClient client) {
        tabViewClient = (client != null) ? client : sDefViewClient;
    }

    void setTabChromeClient(@Nullable final TabChromeClient client) {
        tabChromeClient = (client != null) ? client : sDefChromeClient;
    }

    void setDownloadCallback(@Nullable final DownloadCallback callback) {
        downloadCallback = callback;
    }

    public void setBlockingEnabled(final boolean enabled) {
        if (tabView != null) {
            tabView.setBlockingEnabled(enabled);
        }
    }

    /**
     * To detach @see{android.view.View} of this tab, if any, is detached from its parent.
     */
    public void detach() {
        final boolean hasParentView = (tabView != null)
                && (tabView.getView() != null)
                && (tabView.getView().getParent() != null);
        if (hasParentView) {
            ViewGroup parent = (ViewGroup) tabView.getView().getParent();
            parent.removeView(tabView.getView());
        }
    }

    /* package */ void destroy() {
        setDownloadCallback(null);
        setTabViewClient(null);

        if (tabView != null) {
            // ensure the view not bind to parent
            detach();

            tabView.destroy();
        }
    }

    /* package */ void resume() {
        if (tabView != null) {
            tabView.onResume();
        }
    }

    /* package */ void pause() {
        if (tabView != null) {
            tabView.onPause();
        }
    }

    /* package */ void setParentId(@Nullable String id) {
        this.tabModel.setParentId(id);
    }

    @Nullable
    /* package */ String getParentId() {
        return this.tabModel.getParentId();
    }

    /* package */ void setTitle(@NonNull String title) {
        tabModel.setTitle(title);
    }

    /* package */ void setUrl(@NonNull String url) {
        tabModel.setUrl(url);
    }

    /* package */ void setFavicon(Bitmap icon) {
        tabModel.setFavicon(icon);
    }

    /* package */ TabView initializeView(@NonNull final Activity activity) {
        final String url = this.getUrl(); // fallback for restoring tab
        if (tabView == null) {
            // FIXME: this casting does not make enough sense. TabView and View is totally different
            tabView = (TabView) WebViewProvider.create(activity, null);

            tabView.setViewClient(tabViewClient);
            tabView.setChromeClient(tabChromeClient);
            tabView.setDownloadCallback(downloadCallback);

            if (tabModel.getWebViewState() != null) {
                tabView.restoreViewState(tabModel.getWebViewState());
            } else if (!TextUtils.isEmpty(url)) {
                tabView.loadUrl(url);
            }
        }

        return tabView;
    }

    // TODO: not implement completely
    private void updateThumbnail() {
        if (tabView != null) {
            final View view = tabView.getView();
            view.setDrawingCacheEnabled(true);
            final Bitmap bitmap = tabView.getDrawingCache(true);
            if (bitmap != null) {
                tabModel.setThumbnail(Bitmap.createBitmap(bitmap));
                bitmap.recycle();
            }
            view.setDrawingCacheEnabled(false);
        }
    }

    // TODO: not implement completely
    private Bitmap getThumbnail() {
        return tabModel.getThumbnail();
    }
}
