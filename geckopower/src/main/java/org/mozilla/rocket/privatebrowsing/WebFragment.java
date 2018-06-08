package org.mozilla.rocket.privatebrowsing;/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

import android.app.Activity;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import org.mozilla.focus.tabs.TabChromeClient;
import org.mozilla.focus.tabs.TabView;
import org.mozilla.focus.tabs.TabViewClient;
import org.mozilla.focus.utils.UrlUtils;
import org.mozilla.rocket.geckopower.R;
import org.mozilla.tabs.gecko.GeckoViewProvider;

/**
 * Base implementation for fragments that use SINGLE TabView instance. Based on Android's WebViewFragment.
 */
public class WebFragment extends Fragment {

    private static final int BUNDLE_MAX_SIZE = 300 * 1000; // 300K

    private ProgressBar progressView;
    private ViewGroup contentViewSlot;
    private View webContentView;
    private TabView tabView;

    private Bundle webViewState;

    private static final String ARGUMENT_URL = "url";

    /* If fragment exists but no WebView to use, store url here if there is any loadUrl requirement */
    protected String pendingUrl = null;

    public static WebFragment create(String url) {
        Bundle arguments = new Bundle();
        arguments.putString(ARGUMENT_URL, url);

        WebFragment fragment = new WebFragment();
        fragment.setArguments(arguments);

        return fragment;
    }

    /**
     * Inflate a layout for this fragment. The layout needs to contain a view implementing TabView
     * with the id set to "webview".
     */
    @NonNull
    public View inflateLayout(LayoutInflater inflater,
                              @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_web, container, false);
        progressView = (ProgressBar) view.findViewById(R.id.progress);
        contentViewSlot = (ViewGroup) view.findViewById(R.id.content_view_slot);
        return view;
    }

    /**
     * Get the initial URL to load after the view has been created.
     */
    @Nullable
    public String getInitialUrl() {
        return getArguments().getString(ARGUMENT_URL);
    }

    @Override
    public final View onCreateView(LayoutInflater inflater,
                                   @Nullable ViewGroup container,
                                   @Nullable Bundle savedInstanceState) {

        final View view = inflateLayout(inflater, container, savedInstanceState);

        tabView = createTabView(getActivity());
        tabView.setViewClient(new PrivateTabViewClient());
        tabView.setChromeClient(new PrivateTabChromeClient());

        webContentView = tabView.getView();
        contentViewSlot.addView(webContentView);

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
                tabView.restoreViewState(webViewState);
            }

            final String url = (webViewState == null) ? getInitialUrl() : pendingUrl;
            if (!TextUtils.isEmpty(url)) {
                loadUrl(url);
            }
        } else {
            // Fragment was destroyed
            tabView.restoreViewState(savedInstanceState);
        }
    }

    @Override
    public void onPause() {
        tabView.onPause();

        super.onPause();
    }

    @Override
    public void onResume() {
        tabView.onResume();

        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        tabView.saveViewState(outState);

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
        // only remove tabView in onDestroy since saveViewState access tabView
        // and the callback might be invoked before onDestroyView
        if (tabView != null) {
            tabView.onDetach();
            tabView.destroy();
            final View v = tabView.getView();
            if (v != null && v.getParent() != null) {
                ((ViewGroup) v.getParent()).removeView(v);
            }
            tabView = null;
        }

        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        if (tabView != null) {
            tabView.setViewClient(null);

            // If Fragment is detached from Activity but not be destroyed, saveViewState won't be
            // called. In this case we must store tabView-state manually, to retain browsing history.
            webViewState = new Bundle();
            tabView.saveViewState(webViewState);
        }

        super.onDestroyView();
    }

    /**
     * Let WebView to open the url
     *
     * @param url the url to open. It should be a valid url otherwise WebView won't load anything.
     */
    public void loadUrl(@NonNull final String url) {
        if (tabView != null) {
            if (UrlUtils.isUrl(url)) {
                this.pendingUrl = null; // clear pending url

                // in case of any unexpected path to here, to normalize URL in beta/release build
                final String target = url;
                tabView.loadUrl(target);
            }
        } else {
            this.pendingUrl = url;
        }
    }

    public void goBack() {
        if (tabView != null) {
            tabView.goBack();
        }
    }

    public boolean canGoBack() {
        return tabView != null && tabView.canGoBack();
    }

    private TabView createTabView(@NonNull final Activity activity) {
        GeckoViewProvider.preload(activity.getApplicationContext());
        return GeckoViewProvider.create(activity);
    }

    private class PrivateTabViewClient extends TabViewClient {

        @Override
        public void onPageStarted(final String url) {
            progressView.announceForAccessibility(getString(R.string.accessibility_announcement_loading));

            progressView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageFinished(boolean isSecure) {
            progressView.announceForAccessibility(getString(R.string.accessibility_announcement_loading_finished));

            progressView.setVisibility(View.INVISIBLE);

            if (webContentView.getVisibility() != View.VISIBLE) {
                webContentView.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public boolean handleExternalUrl(final String url) {
            return false;
        }
    }

    private class PrivateTabChromeClient extends TabChromeClient {
        @Override
        public boolean onCreateWindow(boolean isDialog, boolean isUserGesture, Message msg) {
            return false;
        }

        @Override
        public void onProgressChanged(int progress) {
            progressView.setProgress(progress);
        }

        @Override
        public void onReceivedTitle(TabView view, String title) {
        }

    }
}
