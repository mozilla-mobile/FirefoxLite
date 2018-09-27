/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.mozilla.focus.locale.LocaleAwareFragment;
import org.mozilla.focus.utils.AppConstants;
import org.mozilla.focus.utils.SupportUtils;

/**
 * Base implementation for fragments that use a WebView instance. Based on Android's WebViewFragment.
 */
public abstract class DefaultWebFragment extends LocaleAwareFragment {

    private static final int BUNDLE_MAX_SIZE = 300 * 1000; // 300K

    protected WebView webView;
    // webView is not available after onDestroyView, but we need webView reference in callback
    // onSaveInstanceState. However the callback might be invoked at anytime before onDestroy
    // therefore webView-available-state is decided by this flag but not webView reference itself.
    private boolean isWebViewAvailable;

    private Bundle webViewState;

    /* If fragment exists but no WebView to use, store url here if there is any loadUrl requirement */
    private String pendingUrl = null;

    /**
     * Inflate a layout for this fragment.
     */
    @NonNull
    public abstract View inflateLayout(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState);

    public abstract WebViewClient createWebViewClient();

    public abstract WebChromeClient createWebChromeClient();

    /**
     * Get the initial URL to load after the view has been created.
     */
    @Nullable
    public abstract String getInitialUrl();

    @Override
    public final View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflateLayout(inflater, container, savedInstanceState);

        isWebViewAvailable = true;
        WebViewClient webViewClient = createWebViewClient();
        webView.setWebViewClient(webViewClient);
        webView.setWebChromeClient(createWebChromeClient());


        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // restore WebView state
        if (savedInstanceState == null) {
            // in two cases we won't have saved-state: fragment just created, or fragment re-attached.
            // if fragment was detached before, we will have webViewState.
            // per difference case, we should load initial url or pending url(if any).
            if (webViewState != null) {
                webView.restoreState(webViewState);
            }

            final String url = (webViewState == null) ? getInitialUrl() : pendingUrl;
            if (!TextUtils.isEmpty(url)) {
                loadUrl(url);
            }
        } else {
            // Fragment was destroyed
            webView.restoreState(savedInstanceState);
        }
    }

    @Override
    public void applyLocale() {
        // We create and destroy a new WebView here to force the internal state of WebView to know
        // about the new language. See issue #666.
        final WebView unneeded = new WebView(getContext());
        unneeded.destroy();
    }

    @Override
    public void onPause() {
        webView.onPause();

        super.onPause();
    }

    @Override
    public void onResume() {
        webView.onResume();

        super.onResume();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        webView.saveState(outState);

        // Workaround for #1107 TransactionTooLargeException
        // since Android N, system throws a exception rather than just a warning(then drop bundle)
        // To set a threshold for dropping WebView state manually
        // refer: https://issuetracker.google.com/issues/37103380
        final String key = "WEBVIEW_CHROMIUM_STATE";
        if (outState.containsKey(key)) {
            final int size = outState.getByteArray(key).length;
            if (size > BUNDLE_MAX_SIZE) {
                outState.remove(key);
            }
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        // only remove webView in onDestroy since onSaveInstanceState access webView
        // and the callback might be invoked before onDestroyView
        if (webView != null) {
            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        isWebViewAvailable = false;

        if (webView != null) {
            webView.setWebViewClient(null);
            webView.setWebChromeClient(null);

            // If Fragment is detached from Activity but not be destroyed, onSaveInstanceState won't be
            // called. In this case we must store webView-state manually, to retain browsing history.
            webViewState = new Bundle();
            webView.saveState(webViewState);
        }

        super.onDestroyView();
    }

    /**
     * Let WebView to open the url
     *
     * @param url the url to open. It should be a valid url otherwise WebView won't load anything.
     */
    public void loadUrl(@NonNull final String url) {
        final WebView webView = getWebView();
        if (webView != null) {
            if (SupportUtils.isUrl(url)) {
                this.pendingUrl = null; // clear pending url

                // in case of any unexpected path to here, to normalize URL in beta/release build
                final String target = AppConstants.isDevBuild() ? url : SupportUtils.normalize(url);
                webView.loadUrl(target);
            } else if (AppConstants.isDevBuild()) {
                // throw exception to highlight this issue, except release build.
                throw new RuntimeException("trying to open a invalid url: " + url);
            }
        } else {
            this.pendingUrl = url;
        }
    }

    @Nullable
    protected WebView getWebView() {
        return isWebViewAvailable ? webView : null;
    }
}