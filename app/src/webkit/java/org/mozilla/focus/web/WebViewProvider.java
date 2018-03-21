/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.web;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;

import org.mozilla.focus.R;
import org.mozilla.focus.utils.Settings;
import org.mozilla.focus.webkit.TrackingProtectionWebViewClient;
import org.mozilla.focus.webkit.WebkitView;

/**
 * WebViewProvider for creating a WebKit based IWebVIew implementation.
 */
public class WebViewProvider {

    private static String userAgentString = null;

    /**
     * Preload webview data. This allows the webview implementation to load resources and other data
     * it might need, in advance of intialising the view (at which time we are probably wanting to
     * show a website immediately).
     */
    public static void preload(final Context context) {
        TrackingProtectionWebViewClient.triggerPreload(context);
    }

    public static View create(Context context, AttributeSet attrs) {
        WebView.enableSlowWholeDocumentDraw();
        final WebkitView webkitView = new WebkitView(context, attrs);
        final WebSettings settings = webkitView.getSettings();

        setupView(webkitView);
        configureDefaultSettings(context, settings, webkitView);
        applyAppSettings(context, settings);

        return webkitView;
    }

    private static void setupView(WebView webView) {
        webView.setVerticalScrollBarEnabled(true);
        webView.setHorizontalScrollBarEnabled(true);
    }

    @SuppressLint("SetJavaScriptEnabled") // We explicitly want to enable JavaScript
    private static void configureDefaultSettings(Context context, WebSettings settings, WebkitView webkitView) {
        settings.setJavaScriptEnabled(true);

        // Enabling built in zooming shows the controls by default
        settings.setBuiltInZoomControls(true);

        // So we hide the controls after enabling zooming
        settings.setDisplayZoomControls(false);

        // To respect the html viewport:
        settings.setLoadWithOverviewMode(true);

        // Also increase text size to fill the viewport (this mirrors the behaviour of Firefox,
        // Chrome does this in the current Chrome Dev, but not Chrome release).
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);

        // Disable access to arbitrary local files by webpages - assets can still be loaded
        // via file:///android_asset/res, so at least error page images won't be blocked.
        settings.setAllowFileAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setUserAgentString(getUserAgentString(context));

        // Right now I do not know why we should allow loading content from a content provider
        settings.setAllowContentAccess(false);

        // The default for those settings should be "false" - But we want to be explicit.
        settings.setAppCacheEnabled(false);
        settings.setDatabaseEnabled(false);
        settings.setDomStorageEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);

        // We do implement the callbacks - So let's enable it.
        settings.setGeolocationEnabled(true);

        // We do not want to save any data...
        settings.setSaveFormData(true);
        //noinspection deprecation - This method is deprecated but let's call it in case WebView implementations still obey it.
        settings.setSavePassword(false);

        // We have multi-tabs support
        settings.setSupportMultipleWindows(true);

        // We accept third party cookies
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webkitView, true);
        }
    }

    public static void applyAppSettings(Context context, WebSettings settings) {
        // We could consider calling setLoadsImagesAutomatically() here too (This will block images not loaded over the network too)
        settings.setBlockNetworkImage(Settings.getInstance(context).shouldBlockImages());
        settings.setLoadsImagesAutomatically(!Settings.getInstance(context).shouldBlockImages());
    }

    /**
     * Build the browser specific portion of the UA String, based on the webview's existing UA String.
     */
    @VisibleForTesting
    static String getUABrowserString(final String existingUAString, final String focusToken) {
        // Use the default WebView agent string here for everything after the platform, but insert
        // Focus in front of Chrome.
        // E.g. a default webview UA string might be:
        // Mozilla/5.0 (Linux; Android 7.1.1; Pixel XL Build/NOF26V; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/56.0.2924.87 Mobile Safari/537.36
        // And we reuse everything from AppleWebKit onwards, except for adding Focus.
        int start = existingUAString.indexOf("AppleWebKit");
        if (start == -1) {
            // I don't know if any devices don't include AppleWebKit, but given the diversity of Android
            // devices we should have a fallback: we search for the end of the platform String, and
            // treat the next token as the start:
            start = existingUAString.indexOf(")") + 2;

            // If this was located at the very end, then there's nothing we can do, so let's just
            // return focus:
            if (start >= existingUAString.length()) {
                return focusToken;
            }
        }

        final String[] tokens = existingUAString.substring(start).split(" ");

        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].startsWith("Chrome")) {
                tokens[i] = focusToken + " " + tokens[i];

                return TextUtils.join(" ", tokens);
            }
        }

        // If we didn't find a chrome token, we just append the focus token at the end:
        return TextUtils.join(" ", tokens) + " " + focusToken;
    }

    // Warning: WebSettings.getDefaultUserAgent() is a heavy function which runs for 120ms+ on my pixel
    private static String buildUserAgentString(final Context context, final String appName) {
        return buildUserAgentString(context, WebSettings.getDefaultUserAgent(context), appName);
    }

    @VisibleForTesting
    static String buildUserAgentString(final Context context, final String existingWebViewUA, final String appName) {
        final StringBuilder uaBuilder = new StringBuilder();

        // WebView by default includes "; wv" as part of the platform string, but we're a full browser
        // so we shouldn't include that. Replace with "rv" fix some webcompat issue.

        uaBuilder.append(existingWebViewUA.substring(0, existingWebViewUA.indexOf("wv) ") + 4).replace("wv) ", "rv) "));

        final String appVersion;
        try {
            appVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            // This should be impossible - we should always be able to get information about ourselves:
            throw new IllegalStateException("Unable find package details for Rocket", e);
        }

        final String focusToken = appName + "/" + appVersion;
        uaBuilder.append(getUABrowserString(existingWebViewUA, focusToken));
        return uaBuilder.toString();
    }

    // We're caching the ua since buildUserAgentString is pretty heavy.
    public static String getUserAgentString(Context context) {
        if (userAgentString == null) {
            userAgentString = buildUserAgentString(context, context.getResources().getString(R.string.useragent_appname));
        }
        return userAgentString;
    }
}
