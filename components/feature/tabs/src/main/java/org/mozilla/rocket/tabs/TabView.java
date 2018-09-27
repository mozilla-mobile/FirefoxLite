/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.tabs;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;

import org.mozilla.rocket.tabs.web.DownloadCallback;

public interface TabView {
    class HitTarget {
        public final TabView source;
        public final boolean isLink;
        public final String linkURL;

        public final boolean isImage;
        public final String imageURL;

        public HitTarget(@NonNull final TabView source,
                         final boolean isLink,
                         final String linkURL,
                         final boolean isImage,
                         final String imageURL) {

            if (isLink && linkURL == null) {
                throw new IllegalStateException("link hittarget must contain URL");
            }

            if (isImage && imageURL == null) {
                throw new IllegalStateException("image hittarget must contain URL");
            }

            this.source = source;
            this.isLink = isLink;
            this.linkURL = linkURL;
            this.isImage = isImage;
            this.imageURL = imageURL;
        }
    }

    interface FullscreenCallback {
        void fullScreenExited();
    }

    interface FindListener {
        void onFindResultReceived(int activeMatchOrdinal, int numberOfMatches, boolean isDoneCounting);
    }

    /**
     * Enable/Disable content blocking for this session (Only the blockers that are enabled in the app's settings will be turned on/off).
     */
    void setContentBlockingEnabled(boolean enabled);

    /**
     * Be invoked by TabChromeClient.onCreateWindow to transport this new-created-window to its parent window.
     *
     * @param msg The message to send when once a new WebView has been created
     */
    void bindOnNewWindowCreation(@NonNull final Message msg);

    void setImageBlockingEnabled(boolean enabled);

    boolean isBlockingEnabled();

    void performExitFullScreen();

    void setViewClient(@Nullable TabViewClient viewClient);

    void setChromeClient(@Nullable TabChromeClient chromeClient);

    void setDownloadCallback(DownloadCallback callback);

    void setFindListener(FindListener callback);

    void onPause();

    void onResume();

    void destroy();

    void reload();

    void stopLoading();

    String getUrl();

    String getTitle();

    @SiteIdentity.SecurityState
    int getSecurityState();

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
