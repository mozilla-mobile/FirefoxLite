/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment;

import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import org.mozilla.focus.R;
import org.mozilla.focus.utils.IntentUtils;
import org.mozilla.focus.web.WebViewProvider;
import org.mozilla.rocket.tabs.TabChromeClient;
import org.mozilla.rocket.tabs.TabView;
import org.mozilla.rocket.tabs.TabViewClient;

public class InfoFragment extends WebFragment {
    private ProgressBar progressView;
    private ViewGroup webViewSlot;
    private View webView;

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
        progressView = (ProgressBar) view.findViewById(R.id.progress);
        webViewSlot = (ViewGroup) view.findViewById(R.id.webview_slot);
        webView = ((TabView) WebViewProvider.create(getContext(), null)).getView();
        webViewSlot.addView(webView);

        final String url = getInitialUrl();
        if (!(url.startsWith("http://") || url.startsWith("https://"))) {
            // Hide webview until content has loaded, if we're loading built in about/rights/etc
            // pages: this avoid a white flash (on slower devices) between the screen appearing,
            // and the about/right/etc content appearing. We don't do this for SUMO and other
            // external pages, because they are both light-coloured, and significantly slower loading.
            webView.setVisibility(View.INVISIBLE);
        }

        return view;
    }

    @Override
    public TabViewClient createTabViewClient() {
        return new TabViewClient() {

            @Override
            public void onPageStarted(final String url) {
                progressView.announceForAccessibility(getString(R.string.accessibility_announcement_loading));

                progressView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(boolean isSecure) {
                progressView.announceForAccessibility(getString(R.string.accessibility_announcement_loading_finished));

                progressView.setVisibility(View.INVISIBLE);

                if (webView.getVisibility() != View.VISIBLE) {
                    webView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public boolean handleExternalUrl(final String url) {
                final TabView tabView = getTabView();

                return tabView != null && IntentUtils.handleExternalUri(getContext(), url);
            }
        };
    }

    @Override
    public TabChromeClient createTabChromeClient() {
        return new TabChromeClient() {
            @Override
            public boolean onCreateWindow(boolean isDialog, boolean isUserGesture, Message msg) {
                return false;
            }

            @Override
            public void onProgressChanged(int progress) {
                progressView.setProgress(progress);
            }

        };
    }

    @Nullable
    @Override
    public String getInitialUrl() {
        return getArguments().getString(ARGUMENT_URL);
    }

    public void goBack() {
        final TabView tabView = getTabView();
        if (tabView != null) {
            tabView.goBack();
        }
    }

    public boolean canGoBack() {
        final TabView tabView = getTabView();
        return tabView != null && tabView.canGoBack();
    }
}
