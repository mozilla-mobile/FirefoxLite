/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.tabs;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;

/**
 * An interface for listener which cares about changes of tabs to update browser chrome.
 * This is similar to android.webkit.WebChromeClient, but also indicate which tab triggered callback.
 * <p>
 * Every methods of this listener are supposed to run in main thread.
 */
public interface TabsChromeListener {

    int FACTOR_UNKNOWN = 1;
    int FACTOR_TAB_ADDED = 2;
    int FACTOR_TAB_REMOVED = 3;
    int FACTOR_TAB_SWITCHED = 4;
    int FACTOR_NO_FOCUS = 5;
    int FACTOR_BACK_EXTERNAL = 6;

    @IntDef({FACTOR_UNKNOWN,
            FACTOR_TAB_ADDED,
            FACTOR_TAB_REMOVED,
            FACTOR_TAB_SWITCHED,
            FACTOR_NO_FOCUS,
            FACTOR_BACK_EXTERNAL})
    @interface Factor {
    }

    /**
     * @see android.webkit.WebChromeClient#onProgressChanged(android.webkit.WebView, int)
     */
    void onProgressChanged(@NonNull Session tab, int progress);

    /**
     * @see android.webkit.WebChromeClient#onReceivedTitle(android.webkit.WebView, String)
     */
    void onReceivedTitle(@NonNull Session tab, String title);

    /**
     * @see android.webkit.WebChromeClient#onReceivedIcon(android.webkit.WebView, Bitmap)
     */
    void onReceivedIcon(@NonNull Session tab, Bitmap icon);

    /**
     * Notify the host application a tab becomes 'focused tab'. It usually happens when adding,
     * removing or switching tabs.
     *
     * @param tab    The tab becomes focused, null means there is no focused tab
     * @param factor the potential factor which cause this focus-change-event
     */
    void onFocusChanged(@Nullable Session tab, @Factor int factor);

    /**
     * Notify the host application there is a tab be added.
     *
     * @param tab       the tab be added
     * @param arguments the same arguments when invoke @see{org.mozilla.focus.tabs.SessionManager#addTab}.
     *                  It might be null if this tab is not created from the method.
     */
    void onTabAdded(@NonNull Session tab, @Nullable Bundle arguments);

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
    void onLongPress(@NonNull Session tab, TabView.HitTarget hitTarget);

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
    void onEnterFullScreen(@NonNull Session tab,
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
    void onExitFullScreen(@NonNull Session tab);

    /**
     * Notify the host application to show a file chooser. Usually for file uploading.
     *
     * @param tab               The tab which is asking file chooser.
     * @param tabView
     * @param filePathCallback
     * @param fileChooserParams
     * @see android.webkit.WebChromeClient#onShowFileChooser(android.webkit.WebView, ValueCallback, WebChromeClient.FileChooserParams)
     */
    boolean onShowFileChooser(@NonNull Session tab,
                              TabView tabView,
                              ValueCallback<Uri[]> filePathCallback,
                              WebChromeClient.FileChooserParams fileChooserParams);

    /**
     * @see android.webkit.WebChromeClient#onGeolocationPermissionsShowPrompt(String, GeolocationPermissions.Callback)
     */
    void onGeolocationPermissionsShowPrompt(@NonNull Session tab,
                                            String origin,
                                            GeolocationPermissions.Callback callback);

}
