/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.tabs;

import androidx.annotation.NonNull;

/**
 * An interface for listener to receive notifications and requests from Tabs.
 * This is similar to android.webkit.WebViewClient, but also indicate which tab triggered callback.
 * <p>
 * Every methods of this listener are supposed to run in main thread.
 */
public interface TabsViewListener {

    void onTabStarted(@NonNull Session tab);

    void onTabFinished(@NonNull Session tab, boolean isSecure);

    void onURLChanged(@NonNull Session tab, String url);

    /**
     * Subsequent process after WebViewClient.shouldOverrideUrlLoading. TabView implementation will
     * decide whether this function be invoke or not.
     *
     * @param url External url to handle
     * @return true if this Listener already handled the external url
     */
    boolean handleExternalUrl(String url);

    /**
     * Subsequent process after WebViewClient.onReceivedError.
     *
     * @param url             The url that failed to load.
     * @param updateFromError To indicate whether this callback is invoked under error. If page started loading, this value would be true.
     * @return true Return true if the URL was handled, false if we should continue loading the current URL.
     */
    void updateFailingUrl(@NonNull Session tab, String url, boolean updateFromError);
}
