/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tabs.gecko;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.webkit.WebSettings;

import org.mozilla.focus.tabs.TabView;
import org.mozilla.focus.tabs.TabViewProvider;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.GeckoView;

/**
 * WebViewProvider implementation for creating a Gecko based implementation of TabView.
 */
public class GeckoViewProvider implements TabViewProvider {

    private static volatile GeckoRuntime geckoRuntime;
    private static GeckoView gv;
    private Activity activity;

    private static String userAgentString = null;

    public GeckoViewProvider(Activity activity) {
        this.activity = activity;
    }

    public static void preload(final Context context) {
        createGeckoRuntimeIfNeeded(context);
    }

    public TabView create() {
        if (gv == null) {
            gv = new NestedGeckoView(activity, null);
        }
        return new GeckoWebView(gv, geckoRuntime);
    }

    public static TabView create(Activity activity) {
        createGeckoRuntimeIfNeeded(activity);
        GeckoView newGv = new NestedGeckoView(activity, null);
        return new GeckoWebView(newGv, geckoRuntime);
    }

    @Override
    public int getEngineType() {
        return TabViewProvider.ENGINE_GECKO;
    }

    private static void createGeckoRuntimeIfNeeded(Context context) {
        if (geckoRuntime == null) {
            final GeckoRuntimeSettings.Builder runtimeSettingsBuilder =
                    new GeckoRuntimeSettings.Builder();
            runtimeSettingsBuilder.useContentProcessHint(true);
            geckoRuntime = GeckoRuntime.create(context.getApplicationContext(), runtimeSettingsBuilder.build());
        }
    }

    // We're caching the ua since buildUserAgentString is pretty heavy.
    public static String getUserAgentString(Context context) {
        if (userAgentString == null) {
            // FIXME: do not hard code app name
            userAgentString = buildUserAgentString(context, "Rocket");
        }
        return userAgentString;
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

}
