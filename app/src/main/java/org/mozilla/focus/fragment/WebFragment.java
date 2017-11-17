/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import org.mozilla.focus.R;
import org.mozilla.focus.locale.LocaleAwareFragment;
import org.mozilla.focus.utils.AppConstants;
import org.mozilla.focus.utils.UrlUtils;
import org.mozilla.focus.web.IWebView;
import org.mozilla.focus.webkit.WebkitView;

/**
 * Base implementation for fragments that use an IWebView instance. Based on Android's WebViewFragment.
 */
public abstract class WebFragment extends LocaleAwareFragment {

    private static final int BUNDLE_MAX_SIZE = 300 * 1000; // 300K

    private IWebView webView;
    // webView is not available after onDestroyView, but we need webView reference in callback
    // onSaveInstanceState. However the callback might be invoked at anytime before onDestroy
    // therefore webView-available-state is decided by this flag but not webView reference itself.
    private boolean isWebViewAvailable;

    private Bundle webViewState;

    /* If fragment exists but no WebView to use, store url here if there is any loadUrl requirement */
    protected String pendingUrl = null;

    /**
     * Inflate a layout for this fragment. The layout needs to contain a view implementing IWebView
     * with the id set to "webview".
     */
    @NonNull
    public abstract View inflateLayout(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState);

    public abstract IWebView.Callback createCallback();

    /**
     * Get the initial URL to load after the view has been created.
     */
    @Nullable
    public abstract String getInitialUrl();

    @Override
    public final View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflateLayout(inflater, container, savedInstanceState);

        webView = (IWebView) view.findViewById(R.id.webview);
        isWebViewAvailable = true;
        webView.setCallback(createCallback());

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // restore WebView state
        if (savedInstanceState == null) {
            // in two cases we won't have saved-state: fragment just created, or fragment re-attached.
            // if fragment was detached before, we will have webViewState.
            // per difference case, we should load initial url or pending url(if any).
            if (webViewState != null) {
                webView.restoreWebviewState(webViewState);
            }

            final String url = (webViewState == null) ? getInitialUrl() : pendingUrl;
            if (!TextUtils.isEmpty(url)) {
                loadUrl(url);
            }
        } else {
            // Fragment was destroyed
            webView.restoreWebviewState(savedInstanceState);
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
    public void onSaveInstanceState(Bundle outState) {
        webView.onSaveInstanceState(outState);

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
            webView.setCallback(null);

            // If Fragment is detached from Activity but not be destroyed, onSaveInstanceState won't be
            // called. In this case we must store webView-state manually, to retain browsing history.
            webViewState = new Bundle();
            webView.onSaveInstanceState(webViewState);
        }

        super.onDestroyView();
    }

    /**
     * Let WebView to open the url
     *
     * @param url the url to open. It should be a valid url otherwise WebView won't load anything.
     */
    public void loadUrl(@NonNull final String url) {
        final IWebView webView = getWebView();
        if (webView != null) {
            if (UrlUtils.isUrl(url)) {
                this.pendingUrl = null; // clear pending url

                // in case of any unexpected path to here, to normalize URL in beta/release build
                final String target = AppConstants.isDevBuild() ? url : UrlUtils.normalize(url);
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
    protected IWebView getWebView() {
        return isWebViewAvailable ? webView : null;
    }
}
