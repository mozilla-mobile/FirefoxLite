/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.webkit;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
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
import org.mozilla.focus.telemetry.TelemetryWrapper;
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

    private TabViewClient viewClient;
    private TabChromeClient chromeClient;
    private DownloadCallback downloadCallback;
    private FocusWebViewClient client;
    private final LinkHandler linkHandler;

    private boolean shouldReloadOnAttached = false;

    private String lastNonErrorPageUrl;

    public WebkitView(Context context, AttributeSet attrs) {
        super(context, attrs);

        client = new FocusWebViewClient(getContext().getApplicationContext()) {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (!UrlUtils.isInternalErrorURL(url)) {
                    lastNonErrorPageUrl = url;
                }
                super.onPageStarted(view, url, favicon);
            }
        };

        setWebViewClient(client);
        setWebChromeClient(new FocusWebChromeClient());
        setDownloadListener(createDownloadListener());

        if (BuildConfig.DEBUG) {
            setWebContentsDebuggingEnabled(true);
        }

        setLongClickable(true);

        linkHandler = new LinkHandler(this, this);
        setOnLongClickListener(linkHandler);
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

        client.notifyCurrentURL(desiredURL);

        if (backForwardList != null &&
                backForwardList.getCurrentItem().getUrl().equals(desiredURL)) {
            // restoreState doesn't actually load the current page, it just restores navigation history,
            // so we also need to explicitly reload in this case:
            reload();
        } else {
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
        if (client.isBlockingEnabled() == enable) {
            return;
        }

        client.setBlockingEnabled(enable);

        if (!enable) {
            reloadOnAttached();
        }
    }

    public boolean isBlockingEnabled() {
        return client.isBlockingEnabled();
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
        if (viewClient != null) {
            viewClient = new TabViewClientWrapper(viewClient);
        }
        this.viewClient = viewClient;
        this.client.setViewClient(this.viewClient);
    }

    @Override
    public void setChromeClient(TabChromeClient chromeClient) {
        this.chromeClient = chromeClient;
        client.setViewClient(this.viewClient);
        linkHandler.setChromeClient(this.chromeClient);
    }

    @Override
    public void setDownloadCallback(DownloadCallback callback) {
        this.downloadCallback = callback;
    }

    public void loadUrl(String url) {
        // We need to check external URL handling here - shouldOverrideUrlLoading() is only
        // called by webview when clicking on a link, and not when opening a new page for the
        // first time using loadUrl().
        if (!client.shouldOverrideUrlLoading(this, url)) {
            super.loadUrl(url);
        }

        client.notifyCurrentURL(url);
    }

    @Override
    public String getUrl() {
        return lastNonErrorPageUrl;
    }

    private void reloadOnAttached() {
        if (isAttachedToWindow()) {
            reload();
        } else {
            shouldReloadOnAttached = true;
        }
    }

    @Override
    public @SiteIdentity.SecurityState int getSecurityState() {
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

    private class FocusWebChromeClient extends WebChromeClient {

        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message msg) {
            if (chromeClient != null) {
                return chromeClient.onCreateWindow(isDialog, isUserGesture, msg);
            } else {
                return false;
            }
        }

        @Override
        public void onCloseWindow(WebView view) {
            if (chromeClient != null) {
                chromeClient.onCloseWindow(view);
            }
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            if (chromeClient != null) {
                chromeClient.onProgressChanged(newProgress);
            }
        }

        @Override
        public void onReceivedIcon(WebView view, Bitmap icon) {
            final String url = view.getUrl();
            if (TextUtils.isEmpty(url)) {
                return;
            }

            Bitmap refinedBitmap  = FavIconUtils.getRefinedBitmap(getResources(), icon,
                    FavIconUtils.getRepresentativeCharacter(url));

            Site site = new Site();
            site.setTitle(view.getTitle());
            site.setUrl(url);
            site.setFavIcon(refinedBitmap);
            BrowsingHistoryManager.getInstance().updateLastEntry(site, null);

            if (chromeClient != null) {
                chromeClient.onReceivedIcon(view, refinedBitmap);
            }
        }

        @Override
        public void onShowCustomView(View view, final CustomViewCallback webviewCallback) {
            final FullscreenCallback fullscreenCallback = new FullscreenCallback() {
                @Override
                public void fullScreenExited() {
                    webviewCallback.onCustomViewHidden();
                }
            };

            if (chromeClient != null) {
                chromeClient.onEnterFullScreen(fullscreenCallback, view);
            }
            TelemetryWrapper.browseEnterFullScreenEvent();
        }

        @Override
        public void onHideCustomView() {
            if (chromeClient != null) {
                chromeClient.onExitFullScreen();
            }
            TelemetryWrapper.browseExitFullScreenEvent();
        }


        @Override
        public void onPermissionRequest(PermissionRequest request) {
            super.onPermissionRequest(request);
            TelemetryWrapper.browsePermissionEvent(request.getResources());
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(String origin,
                                                       GeolocationPermissions.Callback glpcallback) {
            TelemetryWrapper.browseGeoLocationPermissionEvent();
            chromeClient.onGeolocationPermissionsShowPrompt(origin, glpcallback);
        }

        @Override
        public void onGeolocationPermissionsHidePrompt() {
            super.onGeolocationPermissionsHidePrompt();
        }

        @Override
        public boolean onShowFileChooser(WebView webView,
                                         ValueCallback<Uri[]> filePathCallback,
                                         WebChromeClient.FileChooserParams fileChooserParams) {

            return chromeClient.onShowFileChooser(webView, filePathCallback, fileChooserParams);
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            if (chromeClient != null) {
                chromeClient.onReceivedTitle(view, title);
            }
        }
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

    private static class TabViewClientWrapper extends TabViewClient {
        final TabViewClient callback;

        TabViewClientWrapper(@NonNull TabViewClient callback) {
            this.callback = callback;
        }

        @Override
        public void onPageStarted(String url) {
            this.callback.onPageStarted(url);
        }

        @Override
        public void onPageFinished(boolean isSecure) {
            this.callback.onPageFinished(isSecure);
        }

        @Override
        public void onURLChanged(String url) {
            this.callback.onURLChanged(url);
        }

        @Override
        public boolean handleExternalUrl(String url) {
            return callback.handleExternalUrl(url);
        }

        @Override
        public void updateFailingUrl(String url, boolean updateFromError) {
            this.callback.updateFailingUrl(url, updateFromError);
        }
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
