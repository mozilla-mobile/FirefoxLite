/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.webkit;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebHistoryItem;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewDatabase;

import org.mozilla.focus.BuildConfig;
import org.mozilla.focus.history.BrowsingHistoryManager;
import org.mozilla.focus.history.model.Site;
import org.mozilla.focus.tabs.SiteIdentity;
import org.mozilla.focus.tabs.TabChromeClient;
import org.mozilla.focus.tabs.TabView;
import org.mozilla.focus.tabs.TabViewClient;
import org.mozilla.focus.utils.AppConstants;
import org.mozilla.focus.utils.FavIconUtils;
import org.mozilla.focus.utils.FileUtils;
import org.mozilla.focus.utils.SupportUtils;
import org.mozilla.focus.utils.ThreadUtils;
import org.mozilla.focus.utils.UrlUtils;
import org.mozilla.focus.web.Download;
import org.mozilla.focus.web.DownloadCallback;
import org.mozilla.focus.web.WebViewProvider;

public class WebkitView extends NestedWebView implements TabView {
    private static final String KEY_CURRENTURL = "currenturl";

    private DownloadCallback downloadCallback;
    private FocusWebViewClient webViewClient;
    private FocusWebChromeClient webChromeClient;
    private final LinkHandler linkHandler;

    private boolean shouldReloadOnAttached = false;

    private String lastNonErrorPageUrl;
    private final ErrorPageDelegate errorPageDelegate;

    private WebViewDebugOverlay debugOverlay;

    public WebkitView(Context context, AttributeSet attrs) {
        super(context, attrs);

        webViewClient = new FocusWebViewClient(getContext().getApplicationContext()) {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (!UrlUtils.isInternalErrorURL(url)) {
                    lastNonErrorPageUrl = url;
                }
                super.onPageStarted(view, url, favicon);
            }
        };
        webViewClient.setErrorPageDelegate((errorPageDelegate = new ErrorPageDelegate(this)));

        webChromeClient = new FocusWebChromeClient(this);
        setWebViewClient(webViewClient);
        setWebChromeClient(webChromeClient);
        setDownloadListener(createDownloadListener());

        if (BuildConfig.DEBUG) {
            setWebContentsDebuggingEnabled(true);
        }

        setLongClickable(true);

        linkHandler = new LinkHandler(this, this);
        setOnLongClickListener(linkHandler);

        debugOverlay = WebViewDebugOverlay.create(context);
        debugOverlay.bindWebView(this);
        webViewClient.setDebugOverlay(debugOverlay);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (shouldReloadOnAttached) {
            shouldReloadOnAttached = false;
            reload();
        }
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        this.errorPageDelegate.onWebViewScrolled(l, t);
        this.debugOverlay.onWebViewScrolled(l, t);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    public void restoreViewState(Bundle savedInstanceState) {
        // We need to have a different method name because restoreState() returns
        // a WebBackForwardList, and we can't overload with different return types:
        final WebBackForwardList backForwardList = restoreState(savedInstanceState);

        // Pages are only added to the back/forward list when loading finishes. If a new page is
        // loading when the Activity is paused/killed, then that page won't be in the list,
        // and needs to be restored separately to the history list. We detect this by checking
        // whether the last fully loaded page (getCurrentItem()) matches the last page that the
        // WebView was actively loading (which was retrieved during saveViewState():
        // WebView.getUrl() always returns the currently loading or loaded page).
        // If the app is paused/killed before the initial page finished loading, then the entire
        // list will be null - so we need to additionally check whether the list even exists.

        final String desiredURL = savedInstanceState.getString(KEY_CURRENTURL);

        // If WebView was connecting to a non-exist host (ie. 1.1.1.1:42), getUrl() returns null
        // in saveViewState. In any cases we can not get desiredURL, no need to load it.
        if (TextUtils.isEmpty(desiredURL)) {
            return;
        }

        webViewClient.notifyCurrentURL(desiredURL);

        WebHistoryItem currentItem;
        if (backForwardList != null && (currentItem = backForwardList.getCurrentItem()) != null) {
            String latestHistoryUrl = currentItem.getUrl();

            if (desiredURL.equals(latestHistoryUrl)) {
                // The last url WebView saved is consistent with what we saved.
                reload();

            } else {
                // Two possible cases:
                // Case 1: last url ends up become an error page, the url saved in back-forward list is an error-url.
                // Case 2: last url is not yet being saved by the WebView (WebView didn't finished loading)

                //noinspection StatementWithEmptyBody, for clearer logic and comment
                if (UrlUtils.isInternalErrorURL(latestHistoryUrl) && desiredURL.equals(getUrl())) {
                    // Case 1: What we get from WebHistoryItem:
                    // WebHistoryItem#getOriginalUrl(): error-url
                    // WebHistoryItem#getUrl(): error-url
                    //
                    // However, after WebView restores this error-url, WebView#getUrl() will instead
                    // return a virtual url given when we called loadDataWithBaseUrl(..., virtualUrl)
                    //
                    // What we get from WebView after error-url is restored:
                    // WebView#getOriginalUrl(): error-url
                    // WebView#getUrl(): virtual-url

                    // The condition here means "WebView and us agree the last page is <virtual-url>,
                    // which however, ends up become an error page.

                } else {
                    // Case 2, we have more up-to-date url than WebView, load it
                    loadUrl(desiredURL);
                }
            }
        } else {
            // WebView saved nothing, so directly load what we recorded
            loadUrl(desiredURL);
        }
    }

    @Override
    public void saveViewState(Bundle outState) {
        super.saveState(outState);
        // See restoreWebViewState() for an explanation of why we need to save this in _addition_
        // to WebView's state
        outState.putString(KEY_CURRENTURL, getUrl());
    }

    @Override
    public void setContentBlockingEnabled(boolean enable) {
        if (webViewClient.isBlockingEnabled() == enable) {
            return;
        }

        webViewClient.setBlockingEnabled(enable);

        if (!enable) {
            reloadOnAttached();
        }
    }

    @Override
    public void bindOnNewWindowCreation(@NonNull Message msg) {
        if (!(msg.obj instanceof WebView.WebViewTransport)) {
            throw new IllegalArgumentException("Message payload is not a WebViewTransport instance");
        }

        final WebView.WebViewTransport transport = (WebView.WebViewTransport) msg.obj;
        transport.setWebView(this);
        msg.sendToTarget();
    }

    public boolean isBlockingEnabled() {
        return webViewClient.isBlockingEnabled();
    }

    @Override
    public void setImageBlockingEnabled(boolean enable) {
        WebSettings settings = getSettings();
        if (enable == settings.getBlockNetworkImage()
                && enable == !settings.getLoadsImagesAutomatically()) {
            return;
        }

        WebViewProvider.applyAppSettings(getContext(), getSettings());

        if (enable) {
            reloadOnAttached();
        }
    }

    @Override
    public void performExitFullScreen() {
        evaluateJavascript("(function() { return document.webkitExitFullscreen(); })();", null);
    }

    @Override
    public void setViewClient(TabViewClient viewClient) {
        this.webViewClient.setViewClient(viewClient);
    }

    @Override
    public void setChromeClient(TabChromeClient chromeClient) {
        linkHandler.setChromeClient(chromeClient);
        this.webChromeClient.setChromeClient(chromeClient);
    }

    @Override
    public void setDownloadCallback(DownloadCallback callback) {
        this.downloadCallback = callback;
    }

    public void loadUrl(String url) {
        debugOverlay.onLoadUrlCalled();

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
        this.debugOverlay.updateHistory();
    }

    @Override
    public void goBack() {
        super.goBack();
        this.debugOverlay.updateHistory();
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

    private void reloadOnAttached() {
        if (isAttachedToWindow()) {
            reload();
        } else {
            shouldReloadOnAttached = true;
        }
    }

    @Override
    public @SiteIdentity.SecurityState
    int getSecurityState() {
        // FIXME: Having certificate doesn't mean the connection is secure, see #1562
        return getCertificate() == null ? SiteIdentity.INSECURE : SiteIdentity.SECURE;
    }

    @Override
    public void cleanup() {
        clearFormData();
        clearHistory();
        clearMatches();
        clearSslPreferences();
        clearCache(true);

        // We don't care about the viewClient - we just want to make sure cookies are gone
        CookieManager.getInstance().removeAllCookies(null);

        WebStorage.getInstance().deleteAllData();

        final WebViewDatabase webViewDatabase = WebViewDatabase.getInstance(getContext());
        // It isn't entirely clear how this differs from WebView.clearFormData()
        webViewDatabase.clearFormData();
        webViewDatabase.clearHttpAuthUsernamePassword();
    }

    public static void deleteContentFromKnownLocations(final Context context) {
        ThreadUtils.postToBackgroundThread(new Runnable() {
            @Override
            public void run() {
                // We call all methods on WebView to delete data. But some traces still remain
                // on disk. This will wipe the whole webview directory.
                FileUtils.deleteWebViewDirectory(context);

                // WebView stores some files in the cache directory. We do not use it ourselves
                // so let's truncate it.
                FileUtils.truncateCacheDirectory(context);
            }
        });
    }

    public void insertBrowsingHistory() {
        final String url = getUrl();
        if (TextUtils.isEmpty(url)) {
            return;
        } else if (SupportUtils.BLANK_URL.equals(url)) {
            return;
        }

        if (!UrlUtils.isHttpOrHttps(url)) {
            return;
        }

        evaluateJavascript("(function() { return document.getElementById('mozillaErrorPage'); })();",
                new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String errorPage) {
                        if (!"null".equals(errorPage)) {
                            return;
                        }

                        Site site = new Site();
                        site.setUrl(url);
                        site.setTitle(getTitle());
                        site.setLastViewTimestamp(System.currentTimeMillis());
                        site.setFavIcon(FavIconUtils.getInitialBitmap(getResources(), null, FavIconUtils.getRepresentativeCharacter(url)));
                        BrowsingHistoryManager.getInstance().insert(site, null);
                    }
                });
    }

    @Override
    public View getView() {
        return this;
    }

    private DownloadListener createDownloadListener() {
        return new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                if (!AppConstants.supportsDownloadingFiles()) {
                    return;
                }

                if (downloadCallback != null) {
                    final Download download = new Download(url, userAgent, contentDisposition, mimetype, contentLength, false);
                    downloadCallback.onDownloadStart(download);
                }
            }
        };
    }
}
