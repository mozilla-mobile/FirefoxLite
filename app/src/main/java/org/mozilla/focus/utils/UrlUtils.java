/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.mozilla.focus.search.SearchEngine;
import org.mozilla.focus.search.SearchEngineManager;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

public class UrlUtils {

    private final static Pattern schemePattern = Pattern.compile("^.+://");

    public static String normalize(@NonNull String input) {
        String trimmedInput = input.trim();
        Uri uri = Uri.parse(trimmedInput);

        // for supported/predefined url, no need to normalize it
        for (final String s : SupportUtils.SUPPORTED_URLS) {
            if (s.equals(input)) {
                return input;
            }
        }

        if (TextUtils.isEmpty(uri.getScheme())) {
            uri = Uri.parse("http://" + trimmedInput);
        }

        return uri.toString();
    }

    /**
     * Is the given string a URL or should we perform a search?
     * <p>
     * TODO: This is a super simple and probably stupid implementation.
     */
    public static boolean isUrl(@Nullable String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }

        final String trimmedUrl = url.trim().toLowerCase();
        if (trimmedUrl.contains(" ")) {
            return false;
        }

        for (final String s : SupportUtils.SUPPORTED_URLS) {
            if (s.equals(url)) {
                return true;
            }
        }

        Uri uri = schemePattern.matcher(url).find()
                ? Uri.parse(url)
                : Uri.parse("http://" + url);

        final String host = TextUtils.isEmpty(uri.getHost()) ? "" : uri.getHost();
        switch (uri.getScheme()) {
            case "http":
            case "https":
                // localhost allows zero dot
                if (!host.contains(".")) {
                    return host.equals("localhost");
                }

                // .a.b.c  and a.b.c. are not allowed
                return !host.startsWith(".") && !host.endsWith(".");
            case "file":
                // only "file" scheme allows empty domain
                return !TextUtils.isEmpty(uri.getPath());
            default:
                //  unknown schema will treated as app link
                return true;
        }
    }

    public static boolean isHttpOrHttps(String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }

        return url.startsWith("http:") || url.startsWith("https:");
    }

    public static boolean isSearchQuery(String text) {
        return text.contains(" ");
    }

    public static String createSearchUrl(Context context, String searchTerm) {
        final SearchEngine searchEngine = SearchEngineManager.getInstance()
                .getDefaultSearchEngine(context);

        return searchEngine.buildSearchUrl(searchTerm);
    }

    public static String stripUserInfo(@Nullable String url) {
        if (TextUtils.isEmpty(url)) {
            return "";
        }

        try {
            URI uri = new URI(url);

            final String userInfo = uri.getUserInfo();
            if (userInfo == null) {
                return url;
            }

            // Strip the userInfo to minimise spoofing ability. This only affects what's shown
            // during browsing, this information isn't used when we start editing the URL:
            uri = new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment());

            return uri.toString();
        } catch (URISyntaxException e) {
            // We might be trying to display a user-entered URL (which could plausibly contain errors),
            // in this case its safe to just return the raw input.
            // There are also some special cases that URI can't handle, such as "http:" by itself.
            return url;
        }
    }

    public static boolean isPermittedResourceProtocol(@NonNull final String scheme) {
        if (TextUtils.isEmpty(scheme)) {
            return false;
        }

        return scheme.startsWith("http") ||
                scheme.startsWith("https") ||
                scheme.startsWith("file") ||
                scheme.startsWith("data");
    }

    public static boolean isSupportedProtocol(@NonNull final String scheme) {
        if (TextUtils.isEmpty(scheme)) {
            return false;
        }
        return isPermittedResourceProtocol(scheme) ||
                scheme.startsWith("error");
    }

    public static boolean isInternalErrorURL(final String url) {
        return "data:text/html;charset=utf-8;base64,".equals(url);
    }

    public static boolean urlsMatchExceptForTrailingSlash(final @NonNull String url1, final @NonNull String url2) {
        int lengthDifference = url1.length() - url2.length();

        if (lengthDifference == 0) {
            // The simplest case:
            return url1.equalsIgnoreCase(url2);
        } else if (lengthDifference == 1) {
            // url1 is longer:
            return url1.charAt(url1.length() - 1) == '/' &&
                    url1.regionMatches(true, 0, url2, 0, url2.length());
        } else if (lengthDifference == -1) {
            return url2.charAt(url2.length() - 1) == '/' &&
                    url2.regionMatches(true, 0, url1, 0, url1.length());
        }

        return false;
    }

    public static String stripCommonSubdomains(@Nullable String host) {
        if (host == null) {
            return null;
        }

        // In contrast to desktop, we also strip mobile subdomains,
        // since its unlikely users are intentionally typing them
        int start = 0;

        if (host.startsWith("www.")) {
            start = 4;
        } else if (host.startsWith("mobile.")) {
            start = 7;
        } else if (host.startsWith("m.")) {
            start = 2;
        }

        return host.substring(start);
    }
}
