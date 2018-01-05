package org.mozilla.focus.utils;
/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */


import android.content.Context;
import android.text.TextUtils;
import android.webkit.WebSettings;
import android.webkit.WebView;

public final class DebugUtils {

    private DebugUtils() {

    }

    public static String loadWebViewVersion(Context context) {
        return loadWebViewVersion(new WebView(context));
    }

    public static String loadWebViewVersion(WebView webvView) {
        final String userAgent = webvView.getSettings().getUserAgentString();
        final String webViewVersion = parseWebViewVersion(userAgent);
        return webViewVersion;
    }

    private static String parseWebViewVersion(String userAgent) {
        if (TextUtils.isEmpty(userAgent)) {
            return "";
        }
        final String separator = "Chrome/";
        final int from = userAgent.lastIndexOf(separator) + separator.length();
        final String webviewVersion = userAgent.substring(from, userAgent.indexOf(" ", from));
        return webviewVersion;
    }

}
