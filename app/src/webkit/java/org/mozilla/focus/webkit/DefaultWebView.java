/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.webkit;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import org.mozilla.focus.BuildConfig;
import org.mozilla.focus.utils.UrlUtils;

public class DefaultWebView extends NestedWebView {

    private TrackingProtectionWebViewClient webViewClient;
    private WebChromeClient webChromeClient;

    private boolean shouldReloadOnAttached = false;

    private String lastNonErrorPageUrl;

    public DefaultWebView(Context context, AttributeSet attrs) {
        super(context, attrs);

        webViewClient = new DefaultWebViewClient(getContext().getApplicationContext()) {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (!UrlUtils.isInternalErrorURL(url)) {
                    lastNonErrorPageUrl = url;
                }
                super.onPageStarted(view, url, favicon);
            }
        };

        webChromeClient = new WebChromeClient();
        setWebViewClient(webViewClient);
        setWebChromeClient(webChromeClient);

        if (BuildConfig.DEBUG) {
            setWebContentsDebuggingEnabled(true);
        }

        setLongClickable(true);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (shouldReloadOnAttached) {
            shouldReloadOnAttached = false;
            reload();
        }
    }

    public void loadUrl(String url) {
        // We need to check external URL handling here - shouldOverrideUrlLoading() is only
        // called by webview when clicking on a link, and not when opening a new page for the
        // first time using loadUrl().
        if (!webViewClient.shouldOverrideUrlLoading(this, url)) {
            super.loadUrl(url);
        }

        webViewClient.notifyCurrentURL(url);
    }

    public void reload() {
        if (UrlUtils.isInternalErrorURL(getOriginalUrl())) {
            super.loadUrl(getUrl());
        } else {
            super.reload();
        }
    }

    @Override
    public void goBack() {
        super.goBack();
    }

    @Override
    public String getUrl() {
        final String currentUrl = super.getUrl();
        if (UrlUtils.isInternalErrorURL(currentUrl)) {
            return lastNonErrorPageUrl;
        } else {
            return currentUrl;
        }
    }
}
