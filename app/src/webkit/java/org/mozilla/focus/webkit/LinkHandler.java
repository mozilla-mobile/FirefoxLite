/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.webkit;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.View;
import android.webkit.WebView;

import org.mozilla.rocket.tabs.TabChromeClient;
import org.mozilla.rocket.tabs.TabView;

/* package */ class LinkHandler implements View.OnLongClickListener {
    private final TabView tabView;
    private final WebView webView;
    private
    @Nullable
    TabChromeClient chromeClient = null;

    public LinkHandler(final TabView tabView, final WebView webView) {
        this.tabView = tabView;
        this.webView = webView;
    }

    public void setChromeClient(final @Nullable TabChromeClient chromeClient) {
        this.chromeClient = chromeClient;
    }

    @Override
    public boolean onLongClick(View v) {
        if (chromeClient == null) {
            return false;
        }

        final WebView.HitTestResult hitTestResult = webView.getHitTestResult();

        switch (hitTestResult.getType()) {
            case WebView.HitTestResult.SRC_ANCHOR_TYPE:
                final String linkURL = hitTestResult.getExtra();
                chromeClient.onLongPress(new TabView.HitTarget(this.tabView, true, linkURL, false, null));
                return true;

            case WebView.HitTestResult.IMAGE_TYPE:
                final String imageURL = hitTestResult.getExtra();
                chromeClient.onLongPress(new TabView.HitTarget(this.tabView, false, null, true, imageURL));
                return true;

            case WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE:
                // hitTestResult.getExtra() contains only the image URL, and not the link
                // URL. Internally, WebView's HitTestData contains both, but they only
                // make it available via requestFocusNodeHref...
                final Message message = new Message();
                message.setTarget(new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        final Bundle data = msg.getData();
                        final String url = data.getString("url");
                        final String src = data.getString("src");

                        if (url == null || src == null) {
                            throw new IllegalStateException("WebView did not supply url or src for image link");
                        }

                        if (chromeClient != null) {
                            chromeClient.onLongPress(new TabView.HitTarget(tabView, true, url, true, src));
                        }
                    }
                });

                webView.requestFocusNodeHref(message);
                return true;

            default:
                return false;
        }
    }
}
