/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.tabs;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import org.mozilla.focus.web.DownloadCallback;

public interface TabView {
    class HitTarget {
        public final boolean isLink;
        public final String linkURL;

        public final boolean isImage;
        public final String imageURL;

        public HitTarget(final boolean isLink, final String linkURL, final boolean isImage, final String imageURL) {
            if (isLink && linkURL == null) {
                throw new IllegalStateException("link hittarget must contain URL");
            }

            if (isImage && imageURL == null) {
                throw new IllegalStateException("image hittarget must contain URL");
            }

            this.isLink = isLink;
            this.linkURL = linkURL;
            this.isImage = isImage;
            this.imageURL = imageURL;
        }
    }

    interface FullscreenCallback {
        void fullScreenExited();
    }

    /**
     * Enable/Disable content blocking for this session (Only the blockers that are enabled in the app's settings will be turned on/off).
     */
    void setBlockingEnabled(boolean enabled);

    boolean isBlockingEnabled();

    void performExitFullScreen();

    void setViewClient(@Nullable TabViewClient viewClient);

    void setChromeClient(@Nullable TabChromeClient chromeClient);

    void setDownloadCallback(DownloadCallback callback);

    void onPause();

    void onResume();

    void destroy();

    void reload();

    void stopLoading();

    String getUrl();

    String getTitle();

    void loadUrl(String url);

    void cleanup();

    void goForward();

    void goBack();

    boolean canGoForward();

    boolean canGoBack();

    void restoreViewState(Bundle inState);

    void saveViewState(Bundle outState);

    void insertBrowsingHistory();

    View getView();

    void buildDrawingCache(boolean autoScale);

    Bitmap getDrawingCache(boolean autoScale);
}