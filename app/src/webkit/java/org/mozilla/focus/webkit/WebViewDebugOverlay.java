/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.webkit;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatTextView;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.webkit.WebBackForwardList;
import android.webkit.WebHistoryItem;
import android.webkit.WebView;
import android.widget.LinearLayout;

import org.mozilla.focus.BuildConfig;
import org.mozilla.focus.utils.UrlUtils;

public class WebViewDebugOverlay {
    private WebView webView;
    private LinearLayout historyList;

    @NonNull
    public static WebViewDebugOverlay create(Context context) {
        if (isSupport()) {
            return new WebViewDebugOverlay(context);
        }
        return new NoOpOverlay(context);
    }

    public static boolean isSupport() {
        return BuildConfig.DEBUG;
    }

    private WebViewDebugOverlay(Context context) {
        if (isEnable()) {
            historyList = new LinearLayout(context);
            historyList.setOrientation(LinearLayout.VERTICAL);
            historyList.setBackgroundColor(Color.parseColor("#bbeebb"));
            historyList.setAlpha(0.8f);
        }
    }

    public void onWebViewScrolled(int left, int top) {
        if (isEnable()) {
            historyList.setTranslationY(top);
            historyList.setTranslationX(left);
        }
    }

    public void updateHistory() {
        if (isEnable()) {
            historyList.removeAllViews();
            WebBackForwardList list = this.webView.copyBackForwardList();
            int size = list.getSize();
            insertHistory("size: " + size, Color.BLACK);
            for (int i = 0; i < size; ++i) {
                WebHistoryItem item = list.getItemAtIndex(i);
                String line = "      " + (i + 1) + ". " + item.getOriginalUrl() + ", " + item.getUrl();

                int color;
                if (list.getCurrentIndex() == i) {
                    color = UrlUtils.isInternalErrorURL(item.getOriginalUrl())
                            || UrlUtils.isInternalErrorURL(item.getUrl())
                            ? Color.RED : Color.BLUE;
                } else {
                    color = Color.BLACK;
                }
                insertHistory(line, color);
            }
        }
    }

    public boolean isEnable() {
        return false;
    }

    public void bindWebView(WebView webView) {
        if (isEnable()) {
            this.webView = webView;
            webView.addView(historyList, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            updateHistory();
        }
    }

    private void insertHistory(String history, int textColor) {
        AppCompatTextView textView = new AppCompatTextView(webView.getContext());
        textView.setTextColor(textColor);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        textView.setText(history);
        historyList.addView(textView);
    }

    private static class NoOpOverlay extends WebViewDebugOverlay {
        private NoOpOverlay(Context context) {
            super(context);
        }

        @Override
        public boolean isEnable() {
            return false;
        }
    }
}
