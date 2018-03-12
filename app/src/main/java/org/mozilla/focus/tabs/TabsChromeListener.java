/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.tabs;

import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

/**
 * An interface for listener which cares about changes of tabs to update browser chrome.
 * This is similar to android.webkit.WebChromeClient, but also indicate which tab triggered callback.
 * <p>
 * Every methods of this listener are supposed to run in main thread.
 */
public interface TabsChromeListener {

    /**
     * @see android.webkit.WebChromeClient#onProgressChanged(WebView, int)
     */
    void onProgressChanged(@NonNull Tab tab, int progress);

    /**
     * @see android.webkit.WebChromeClient#onReceivedTitle(WebView, String)
     */
    void onReceivedTitle(@NonNull Tab tab, String title);

    /**
     * @see android.webkit.WebChromeClient#onReceivedIcon(WebView, Bitmap)
     */
    void onReceivedIcon(@NonNull Tab tab, Bitmap icon);

    /**
     * Notify the host application a tab becomes 'current tab'. It usually happens when adding,
     * removing or switching tabs.
     *
     * @param tab The tab becomes current tab
     */
    void onTabHoist(@NonNull Tab tab);

    /**
     * Notify the host application the total tab counts changed.
     *
     * @param count total tabs count
     */
    void onTabCountChanged(int count);

    /**
     * Notify the host application that long-press happened on a tab
     *
     * @param tab       The tab received long press event
     * @param hitTarget
     */
    void onLongPress(@NonNull Tab tab, TabView.HitTarget hitTarget);

    /**
     * Notify the host application that a tab has entered full screen mode.
     * <p>
     * Some TabView implementations may pass a custom View which contains the web contents in
     * full screen mode.
     *
     * @param tab               The tab which entered fullscreen.
     * @param callback          The callback needs to be invoked to request the page to exit full screen mode.
     * @param fullscreenContent The contentView requested to be displayed in fullscreen
     */
    void onEnterFullScreen(@NonNull Tab tab,
                           @NonNull TabView.FullscreenCallback callback,
                           @Nullable View fullscreenContent);

    /**
     * Notify the host application that the a tab has exited full screen mode.
     * <p>
     * If a View was passed when the application entered full screen mode then this view must
     * be hidden now.
     *
     * @param tab The tab which which existed fullscreen.
     */
    void onExitFullScreen(@NonNull Tab tab);

    /**
     * Notify the host application to show a file chooser. Usually for file uploading.
     *
     * @param tab               The tab which is asking file chooser.
     * @param webView
     * @param filePathCallback
     * @param fileChooserParams
     * @see android.webkit.WebChromeClient#onShowFileChooser(WebView, ValueCallback, WebChromeClient.FileChooserParams)
     */
    boolean onShowFileChooser(@NonNull Tab tab,
                              WebView webView,
                              ValueCallback<Uri[]> filePathCallback,
                              WebChromeClient.FileChooserParams fileChooserParams);

    /**
     * @see android.webkit.WebChromeClient#onGeolocationPermissionsShowPrompt(String, GeolocationPermissions.Callback)
     */
    void onGeolocationPermissionsShowPrompt(@NonNull Tab tab,
                                            String origin,
                                            GeolocationPermissions.Callback callback);

}
