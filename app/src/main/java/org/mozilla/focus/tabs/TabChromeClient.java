/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.tabs;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

/**
 * An abstract layer of @see{android.webkit.WebChromeClient}
 */
public class TabChromeClient {

    public boolean onCreateWindow(boolean isDialog, boolean isUserGesture, Message msg) {
        return false;
    }

    public void onCloseWindow(WebView webView) {
    }

    public void onProgressChanged(int progress) {
    }


    /**
     * @see android.webkit.WebChromeClient
     */
    public boolean onShowFileChooser(WebView webView,
                                     @Nullable ValueCallback<Uri[]> filePathCallback,
                                     @Nullable WebChromeClient.FileChooserParams fileChooserParams) {
        return false;
    }

    public void onReceivedTitle(TabView view, String title) {
    }

    public void onReceivedIcon(WebView view, Bitmap icon) {
    }

    public void onLongPress(TabView.HitTarget hitTarget) {
    }

    /**
     * Notify the host application that the current page has entered full screen mode.
     * <p>
     * The callback needs to be invoked to request the page to exit full screen mode.
     * <p>
     * Some TabView implementations may pass a custom View which contains the web contents in
     * full screen mode.
     */
    public void onEnterFullScreen(@NonNull TabView.FullscreenCallback callback, @Nullable View view) {
    }

    /**
     * Notify the host application that the current page has exited full screen mode.
     * <p>
     * If a View was passed when the application entered full screen mode then this view must
     * be hidden now.
     */
    public void onExitFullScreen() {
    }

    public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
    }
}
