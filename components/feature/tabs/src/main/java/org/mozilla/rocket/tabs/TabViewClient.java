/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.tabs;

/**
 * An abstract layer of @see{android.webkit.WebViewClient}
 */
public interface TabViewClient {

    void onPageStarted(String url);

    void onPageFinished(boolean isSecure) ;

    void onURLChanged(String url);

    /**
     * Return true if the URL was handled, false if we should continue loading the current URL.
     */
    boolean handleExternalUrl(String url);


    void updateFailingUrl(String url, boolean updateFromError) ;
}
