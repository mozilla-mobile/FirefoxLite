/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.focus.webkit;

import android.content.res.Resources;
import android.support.v4.util.ArrayMap;
import android.webkit.WebView;

import org.mozilla.focus.R;
import org.mozilla.focus.utils.HtmlLoader;

import java.util.Map;

public class ErrorPage {


    public static boolean supportsErrorCode(final int errorCode) {
        // in Focus we already render different content for vary errorCode.
        // Zerda's designer thinks we should be user friendly, by showing the same content.
        // So we support every error code!
        // If one day we want to be developer friendly, we should dig source code of Focus.
        return true;
    }

    public static void loadErrorPage(final WebView webView, final String desiredURL, final int errorCode) {
        // This is quite hacky: ideally we'd just load the css file directly using a '<link rel="stylesheet"'.
        // However webkit thinks it's still loading the original page, which can be an https:// page.
        // If mixed content blocking is enabled (which is probably what we want in Focus), then webkit
        // will block file:///android_res/ links from being loaded - which blocks our css from being loaded.
        // We could hack around that by enabling mixed content when loading an error page (and reenabling it
        // once that's loaded), but doing that correctly and reliably isn't particularly simple. Loading
        // the css data and stuffing it into our html is much simpler, especially since we're already doing
        // string substitutions.
        // As an added bonus: file:/// URIs are broken if the app-ID != app package, see:
        // https://code.google.com/p/android/issues/detail?id=211768 (this breaks loading css via file:///
        // references when running debug builds, and probably klar too) - which means this wouldn't
        // be possible even if we hacked around the mixed content issues.
        final String cssString = HtmlLoader.loadResourceFile(webView.getContext(), R.raw.errorpage_style, null);

        final Map<String, String> substitutionMap = new ArrayMap<>();

        final Resources resources = webView.getContext().getResources();

        substitutionMap.put("%page-title%", resources.getString(R.string.error_page_title));
        substitutionMap.put("%button%", resources.getString(R.string.error_page_button));

        substitutionMap.put("%messageShort%", resources.getString(R.string.error_page_title));
        substitutionMap.put("%messageLong%", resources.getString(R.string.error_page_message));

        substitutionMap.put("%css%", cssString);

        final String errorPage = HtmlLoader.loadResourceFile(webView.getContext(), R.raw.errorpage, substitutionMap);

        // We could load the raw html file directly into the webview using a file:///android_res/
        // URI - however we'd then need to do some JS hacking to do our String substitutions. Moreover
        // we'd have to deal with the mixed-content issues detailed above in that case.
        webView.loadDataWithBaseURL(desiredURL, errorPage, "text/html", "UTF8", desiredURL);
    }
}
