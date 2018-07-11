/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.tabs;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import org.mozilla.focus.persistence.TabEntity;
import org.mozilla.focus.web.DownloadCallback;

import java.util.UUID;

public class Tab {

    public static final String ID_EXTERNAL = "_open_from_external_";

    private static final String TAG = "Tab";

    /**
     * A placeholder in case of there is no callback to use.
     */
    private static TabViewClient sDefViewClient = new TabViewClient();
    private static TabChromeClient sDefChromeClient = new TabChromeClient();

    private TabEntity tabEntity;
    private TabView tabView;
    private TabViewClient tabViewClient = sDefViewClient;
    private TabChromeClient tabChromeClient = sDefChromeClient;
    private DownloadCallback downloadCallback;

    public Tab() {
        this(new TabEntity(UUID.randomUUID().toString(), ""));
    }

    public Tab(@NonNull TabEntity model) {
        this.tabEntity = model;
    }

    public TabEntity getSaveModel() {
        if (tabEntity.getWebViewState() == null) {
            tabEntity.setWebViewState(new Bundle());
        }

        if (tabView != null) {
            tabEntity.setTitle(tabView.getTitle());
            tabEntity.setUrl(tabView.getUrl());
            tabView.saveViewState(tabEntity.getWebViewState());
        }

        return tabEntity;
    }

    @Nullable
    public TabView getTabView() {
        return tabView;
    }

    public String getId() {
        return this.tabEntity.getId();
    }

    public String getTitle() {
        if (tabView == null) {
            return tabEntity.getTitle();
        } else {
            return tabView.getTitle();
        }
    }

    public String getUrl() {
        if (tabView == null) {
            if (tabEntity == null) {
                Log.d(TAG, "trying to get url from a tab which has no view nor model");
                return null;
            } else {
                return tabEntity.getUrl();
            }
        } else {
            return tabView.getUrl();
        }
    }

    @Nullable
    public Bitmap getFavicon() {
        if (tabEntity != null) {
            return tabEntity.getFavicon();
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

    public void setContentBlockingEnabled(final boolean enabled) {
        if (tabView != null) {
            tabView.setContentBlockingEnabled(enabled);
        }
    }

    public void setImageBlockingEnabled(boolean enabled) {
        if (tabView != null) {
            tabView.setImageBlockingEnabled(enabled);
        }
    }

    public boolean hasParentTab() {
        return !isFromExternal() && !TextUtils.isEmpty(getParentId());
    }

    public boolean isFromExternal() {
        return ID_EXTERNAL.equals(getParentId());
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
        this.tabEntity.setParentId(id);
    }

    @Nullable
    /* package */ String getParentId() {
        return this.tabEntity.getParentId();
    }

    /* package */ void setTitle(@NonNull String title) {
        tabEntity.setTitle(title);
    }

    /* package */ void setUrl(@NonNull String url) {
        tabEntity.setUrl(url);
    }

    /* package */ void setFavicon(Bitmap icon) {
        tabEntity.setFavicon(icon);
    }

    /* package */ TabView initializeView(@NonNull final TabViewProvider provider) {
        final String url = this.getUrl(); // fallback for restoring tab
        if (tabView == null) {
            tabView = provider.create();

            tabView.setViewClient(tabViewClient);
            tabView.setChromeClient(tabChromeClient);
            tabView.setDownloadCallback(downloadCallback);

            if (tabEntity.getWebViewState() != null) {
                tabView.restoreViewState(tabEntity.getWebViewState());
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
                tabEntity.setThumbnail(Bitmap.createBitmap(bitmap));
                bitmap.recycle();
            }
            view.setDrawingCacheEnabled(false);
        }
    }

    // TODO: not implement completely
    private Bitmap getThumbnail() {
        return tabEntity.getThumbnail();
    }
}
