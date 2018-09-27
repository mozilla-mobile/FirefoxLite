/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.urlutils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import java.net.URI;
import java.net.URISyntaxException;

public class UrlUtils {

    private final static String HTTP_SCHEME_PREFIX = "http://";
    private final static String HTTPS_SCHEME_PREFIX = "https://";
    private final static String[] HTTP_SCHEME_PREFIX_ARRAY = {HTTP_SCHEME_PREFIX, HTTPS_SCHEME_PREFIX};
    private final static String WWW_PREFIX = "www.";
    private final static String MOBILE_PREFIX = "mobile.";
    private final static String M_PREFIX = "m.";
    private final static String[] COMMON_SUBDOMAINS_PREFIX_ARRAY = {WWW_PREFIX, MOBILE_PREFIX, M_PREFIX};

    public static boolean isHttpOrHttps(String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }

        return url.startsWith("http:") || url.startsWith("https:");
    }

    public static boolean isSearchQuery(String text) {
        return text.contains(" ");
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
        return stripPrefix(host, COMMON_SUBDOMAINS_PREFIX_ARRAY);
    }

    public static String stripHttp(@Nullable String host) {
        return stripPrefix(host, HTTP_SCHEME_PREFIX_ARRAY);
    }

    public static String stripPrefix(@Nullable String host, @NonNull String[] prefixArray) {
        if (host == null) {
            return null;
        }
        // In contrast to desktop, we also strip mobile subdomains,
        // since its unlikely users are intentionally typing them
        int start = 0;

        for (String prefix : prefixArray) {
            if (host.startsWith(prefix)) {
                start = prefix.length();
                break;
            }
        }

        return host.substring(start);
    }
}
