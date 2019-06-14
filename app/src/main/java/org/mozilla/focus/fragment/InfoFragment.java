/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import org.mozilla.focus.R;
import org.mozilla.focus.web.WebViewProvider;
import org.mozilla.focus.webkit.DefaultWebViewClient;

public class InfoFragment extends DefaultWebFragment {
    private ProgressBar progressView;

    private static final String ARGUMENT_URL = "url";

    public static InfoFragment create(String url) {
        Bundle arguments = new Bundle();
        arguments.putString(ARGUMENT_URL, url);

        InfoFragment fragment = new InfoFragment();
        fragment.setArguments(arguments);

        return fragment;
    }

    @NonNull
    @Override
    public View inflateLayout(LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_info, container, false);
        progressView = view.findViewById(R.id.progress);
        ViewGroup webViewSlot = view.findViewById(R.id.webview_slot);
        webView = (WebView) WebViewProvider.createDefaultWebView(getContext(), null);
        webViewSlot.addView(webView);

        final String url = getInitialUrl();
        if (!TextUtils.isEmpty(url) && !(url.startsWith("http://") || url.startsWith("https://"))) {
            // Hide webview until content has loaded, if we're loading built in about/rights/etc
            // pages: this avoid a white flash (on slower devices) between the screen appearing,
            // and the about/right/etc content appearing. We don't do this for SUMO and other
            // external pages, because they are both light-coloured, and significantly slower loading.
            webView.setVisibility(View.INVISIBLE);
        }

        return view;
    }

    @Override
    public WebViewClient createWebViewClient() {
        return new DefaultWebViewClient(getContext().getApplicationContext()) {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);

                progressView.announceForAccessibility(getString(R.string.accessibility_announcement_loading));
                progressView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, final String url) {
                super.onPageFinished(view, url);

                progressView.announceForAccessibility(getString(R.string.accessibility_announcement_loading_finished));
                progressView.setVisibility(View.INVISIBLE);

                if (webView.getVisibility() != View.VISIBLE) {
                    webView.setVisibility(View.VISIBLE);
                }
            }
        };
    }

    @Override
    public WebChromeClient createWebChromeClient() {
        return new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message msg) {
                return false;
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressView.setProgress(newProgress);
            }
        };
    }

    @Nullable
    @Override
    public String getInitialUrl() {
        return getArguments().getString(ARGUMENT_URL);
    }

    public void goBack() {
        if (webView != null) {
            webView.goBack();
        }
    }

    public boolean canGoBack() {
        return webView != null && webView.canGoBack();
    }
}
