/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.webkit.WebView;

import org.mozilla.focus.R;
import org.mozilla.focus.locale.Locales;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class SupportUtils {
    public static final String BLANK_URL = "about:blank";
    public static final String FOCUS_ABOUT_URL = "focusabout:";
    public static final String YOUR_RIGHTS_URI = "file:///android_res/raw/rights.html";
    public static final String PRIVACY_URL = "https://www.mozilla.org/privacy/firefox-rocket";
    public static final String ABOUT_URI = "file:///android_res/raw/about.html";

    private final static Pattern schemePattern = Pattern.compile("^.+://");

    static final String[] SUPPORTED_URLS = new String[]{
            BLANK_URL,
            FOCUS_ABOUT_URL,
            YOUR_RIGHTS_URI,
            PRIVACY_URL,
            ABOUT_URI
    };

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

        final String trimmedUrl = url.trim().toLowerCase(Locale.getDefault());
        if (trimmedUrl.contains(" ")) {
            return false;
        }

        for (final String s : SupportUtils.SUPPORTED_URLS) {
            if (s.equals(trimmedUrl)) {
                return true;
            }
        }

        Uri uri = schemePattern.matcher(trimmedUrl).find()
                ? Uri.parse(trimmedUrl)
                : Uri.parse("http://" + trimmedUrl);

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

    public static boolean isTemplateSupportPages(String url) {
        final boolean isTemplate;
        switch (url) {
            case FOCUS_ABOUT_URL:
            case YOUR_RIGHTS_URI:
                isTemplate = true;
                break;
            default:
                isTemplate = false;
                break;
        }
        return isTemplate;
    }

    public static String getSumoURLForTopic(final Context context, final String topic) {
        String escapedTopic;
        try {
            escapedTopic = URLEncoder.encode(topic, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("utf-8 should always be available", e);
        }

        final String appVersion;
        try {
            appVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            // This should be impossible - we should always be able to get information about ourselves:
            throw new IllegalStateException("Unable find package details for Focus", e);
        }

        final String osTarget = "Android";
        final String langTag = Locales.getLanguageTag(Locale.getDefault());

        return "https://support.mozilla.org/1/mobile/" + appVersion + "/" + osTarget + "/" + langTag + "/" + escapedTopic;
    }

    public static String getManifestoURL() {
        final String langTag = Locales.getLanguageTag(Locale.getDefault());
        return "https://www.mozilla.org/" + langTag + "/about/manifesto/";
    }

    public static String getPrivacyURL() {
        return PRIVACY_URL;
    }

    public static String getYourRightsURI() {
        return YOUR_RIGHTS_URI;
    }

    public static String getAboutURI() {
        return ABOUT_URI;
    }

    public static void loadSupportPages(WebView webview, String url) {
        switch (url) {
            case FOCUS_ABOUT_URL:
                loadAbout(webview);
                break;
            case YOUR_RIGHTS_URI:
                loadRights(webview);
                break;
            default:
                throw new IllegalArgumentException("Unknown internal pages url: " + url);
        }
    }

    private static void loadRights(final WebView webView) {
        final Context context = webView.getContext();
        final Resources resources = Locales.getLocalizedResources(webView.getContext());

        final Map<String, String> substitutionMap = new ArrayMap<>();

        final String appName = context.getResources().getString(R.string.app_name);
        final String mozilla = context.getResources().getString(R.string.mozilla);
        final String firefox = context.getResources().getString(R.string.firefox);
        final String mpl = context.getResources().getString(R.string.mpl);
        final String mplUrl = "https://www.mozilla.org/en-US/MPL/";
        final String trademarkPolicyUrl = "https://www.mozilla.org/foundation/trademarks/policy/";
        final String gplUrl = "file:///android_asset/gpl.html";
        final String trackingProtectionUrl = "https://wiki.mozilla.org/Security/Tracking_protection#Lists";
        final String licensesUrl = "file:///android_asset/licenses.html";

        final String content1 = resources.getString(R.string.your_rights_content1, appName, mozilla);
        substitutionMap.put("%your-rights-content1%", content1);

        final String content2 = resources.getString(R.string.your_rights_content2, appName, mplUrl, mpl);
        substitutionMap.put("%your-rights-content2%", content2);

        final String content3 = resources.getString(R.string.your_rights_content3, appName, trademarkPolicyUrl, mozilla, firefox);
        substitutionMap.put("%your-rights-content3%", content3);

        final String content4 = resources.getString(R.string.your_rights_content4, appName, licensesUrl);
        substitutionMap.put("%your-rights-content4%", content4);

        final String content5 = resources.getString(R.string.your_rights_content5, appName, gplUrl, trackingProtectionUrl);
        substitutionMap.put("%your-rights-content5%", content5);

        final String data = HtmlLoader.loadResourceFile(webView.getContext(), R.raw.rights, substitutionMap);
        webView.loadDataWithBaseURL(getYourRightsURI(), data, "text/html", "UTF-8", null);
    }

    private static void loadAbout(final WebView webView) {
        final Context context = webView.getContext();
        final Resources resources = Locales.getLocalizedResources(webView.getContext());

        final String webviewVersion = DebugUtils.loadWebViewVersion(webView.getContext());

        final Map<String, String> substitutionMap = new ArrayMap<>();
        final String appName = webView.getContext().getResources().getString(R.string.app_name);
        final String mozilla = webView.getContext().getResources().getString(R.string.mozilla);
        final String aboutBody = webView.getContext().getResources().getString(R.string.about_content_body, appName, mozilla);

        final String aboutURI = SupportUtils.getAboutURI();
        final String learnMoreURL = SupportUtils.getManifestoURL();
        final String supportURL = SupportUtils.getSumoURLForTopic(webView.getContext(), "rocket-help");
        final String rightURL = SupportUtils.getYourRightsURI();
        final String privacyURL = SupportUtils.getPrivacyURL();

        final String linkLearnMore = resources.getString(R.string.about_link_learn_more);
        final String linkSupport = resources.getString(R.string.about_link_support);
        final String linkYourRights = resources.getString(R.string.about_link_your_rights);
        final String linkPrivacy = resources.getString(R.string.about_link_privacy);

        String aboutVersion = "";
        try {
            aboutVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            // Nothing to do if we can't find the package name.
        }
        substitutionMap.put("%about-version%", aboutVersion);

        final String aboutContent = resources.getString(R.string.about_content
                , aboutBody
                , learnMoreURL
                , linkLearnMore
                , supportURL
                , linkSupport
                , rightURL
                , linkYourRights
                , privacyURL
                , linkPrivacy
        );
        substitutionMap.put("%about-content%", aboutContent);

        final String wordmark = HtmlLoader.loadPngAsDataURI(webView.getContext(), R.drawable.logotype);
        substitutionMap.put("%wordmark%", wordmark);

        substitutionMap.put("%webview-version%", webviewVersion);

        final String data = HtmlLoader.loadResourceFile(webView.getContext(), R.raw.about, substitutionMap);
        // We use a file:/// base URL so that we have the right origin to load file:/// css and
        // image resources.
        webView.loadDataWithBaseURL(aboutURI, data, "text/html", "UTF-8", null);
    }

}
