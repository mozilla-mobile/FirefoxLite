/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.focus.webkit;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.mozilla.focus.tabs.TabViewClient;
import org.mozilla.focus.utils.AppConstants;
import org.mozilla.focus.utils.Settings;
import org.mozilla.focus.utils.SupportUtils;
import org.mozilla.focus.utils.UrlUtils;

/**
 * WebViewClient layer that handles browser specific WebViewClient functionality, such as error pages
 * and external URL handling.
 */
/* package */ class FocusWebViewClient extends TrackingProtectionWebViewClient {
    private final static String ERROR_PROTOCOL = "error:";

    private TabViewClient viewClient;
    private static final String GOOGLE_OAUTH2_PREFIX = "https://accounts.google.com/o/oauth2/";
    private static final String IGNORE_GOOGLE_WEBVIEW_BLOCKING_PARAM = "&suppress_webview_warning=true";

    private WebViewDebugOverlay debugOverlay;
    private ErrorPageDelegate errorPageDelegate;

    FocusWebViewClient(Context context) {
        super(context);
    }

    public void setViewClient(TabViewClient callback) {
        this.viewClient = callback;
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        // Only update the user visible URL if:
        // 1. The purported site URL has actually been requested
        // 2. And it's being loaded for the main frame (and not a fake/hidden/iframe request)
        // Note also: shouldInterceptRequest() runs on a background thread, so we can't actually
        // query WebView.getURL().
        // We update the URL when loading has finished too (redirects can happen after a request has been
        // made in which case we don't get shouldInterceptRequest with the final URL), but this
        // allows us to update the URL during loading.
        if (request.isForMainFrame()) {

            // WebView will always add a trailing / to the request URL, but currentPageURL may or may
            // not have a trailing URL (usually no trailing / when a link is entered via UrlInputFragment),
            // hence we do a somewhat convoluted test:
            final String requestURL = request.getUrl().toString();
            if (currentPageURL != null && UrlUtils.urlsMatchExceptForTrailingSlash(currentPageURL, requestURL)) {
                view.post(new Runnable() {
                    @Override
                    public void run() {
                        if (viewClient != null) {
                            viewClient.onURLChanged(currentPageURL);
                        }
                    }
                });
            }
        }

        return super.shouldInterceptRequest(view, request);
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {

        if (viewClient != null) {
            viewClient.updateFailingUrl(url, false);
            viewClient.onPageStarted(url);
        }

        if (this.errorPageDelegate != null) {
            this.errorPageDelegate.onPageStarted();
        }

        this.debugOverlay.recordLifecycle("onPageStarted:" + url, true);
        super.onPageStarted(view, url, favicon);
    }

    @Override
    public void onPageFinished(WebView view, final String url) {
        if (viewClient != null) {
            viewClient.onPageFinished(view.getCertificate() != null);
        }
        super.onPageFinished(view, url);

        this.debugOverlay.updateHistory();
        this.debugOverlay.recordLifecycle("onPageFinished:" + url, false);
    }

    private static boolean shouldOverrideInternalPages(WebView webView, String url) {
        if (SupportUtils.isTemplateSupportPages(url)) {
            SupportUtils.loadSupportPages(webView, url);
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        view.getSettings().setLoadsImagesAutomatically(true);
        if (url == null) {
            // in case of null url, we won't crash app in release build
            if (AppConstants.isDevBuild()) {
                throw new RuntimeException("Got null url in FocsWebViewClient.shouldOverrideUrlLoading");
            } else {
                return super.shouldOverrideUrlLoading(view, "");
            }
        }

        // A workaround for Google SSO since they're blocking us even when we had changed the agent
        if (url.startsWith(GOOGLE_OAUTH2_PREFIX) && !url.endsWith(IGNORE_GOOGLE_WEBVIEW_BLOCKING_PARAM)) {
            view.loadUrl(url.concat(IGNORE_GOOGLE_WEBVIEW_BLOCKING_PARAM));
            return true;
        }

        if (shouldOverrideInternalPages(view, url)) {
            return true;
        }

        // Allow pages to blank themselves by loading about:blank. While it's a little incorrect to let pages
        // access our internal URLs, Chrome allows loads to about:blank and, to ensure our behavior conforms
        // to the behavior that most of the web is developed against, we do too.
        if (url.equals(SupportUtils.BLANK_URL)) {
            return false;
        }

        // shouldOverrideUrlLoading() is called for both the main frame, and iframes.
        // That can get problematic if an iframe tries to load an unsupported URL.
        // We then try to either handle that URL (ask to open relevant app), or extract
        // a fallback URL from the intent (or worst case fall back to an error page). In the
        // latter 2 cases, we explicitly open the fallback/error page in the main view.
        // Websites probably shouldn't use unsupported URLs in iframes, but we do need to
        // be careful to handle all valid schemes here to avoid redirecting due to such an iframe
        // (e.g. we don't want to redirect to a data: URI just because an iframe shows such
        // a URI).
        // (The API 24+ version of shouldOverrideUrlLoading() lets us determine whether
        // the request is for the main frame, and if it's not we could then completely
        // skip the external URL handling.)
        final Uri uri = Uri.parse(url);
        if (!UrlUtils.isSupportedProtocol(uri.getScheme()) &&
                viewClient != null &&
                viewClient.handleExternalUrl(url)) {
            return true;
        }

        view.getSettings().setLoadsImagesAutomatically(!Settings.getInstance(view.getContext()).shouldBlockImages());
        return super.shouldOverrideUrlLoading(view, url);
    }

    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        super.onReceivedSslError(view, handler, error);
        this.debugOverlay.recordLifecycle("onReceivedSslError:" + error.getUrl(), false);

        // Webkit can try to load the favicon for a bad page when you set a new URL. If we then
        // loadErrorPage() again, webkit tries to load the favicon again. We end up in onReceivedSSlError()
        // again, and we get an infinite loop of reloads (we also erroneously show the favicon URL
        // in the toolbar, but that's less noticeable). Hence we check whether this error is from
        // the desired page, or a page resource:
        if (error.getUrl().equals(currentPageURL)) {
            if (this.errorPageDelegate != null) {
                this.errorPageDelegate.onReceivedSslError(view, handler, error);
            }
        }
    }

    @Override
    public void onReceivedError(final WebView webView, int errorCode,
                                final String description, String failingUrl) {
        if (viewClient != null) {
            viewClient.updateFailingUrl(failingUrl, true);
        }

        this.debugOverlay.recordLifecycle("onReceivedError:" + failingUrl, false);

        // This is a hack: onReceivedError(WebView, WebResourceRequest, WebResourceError) is API 23+ only,
        // - the WebResourceRequest would let us know if the error affects the main frame or not. As a workaround
        // we just check whether the failing URL is the current URL, which is enough to detect an error
        // in the main frame.

        // WebView swallows odd pages and only sends an error (i.e. it doesn't go through the usual
        // shouldOverrideUrlLoading), so we need to handle special pages here:
        // about: urls are even more odd: webview doesn't tell us _anything_, hence the use of
        // a different prefix:
        if (failingUrl.startsWith(ERROR_PROTOCOL)) {
            // format: error:<error_code>
            final int errorCodePosition = ERROR_PROTOCOL.length();
            final String errorCodeString = failingUrl.substring(errorCodePosition);

            int desiredErrorCode;
            try {
                desiredErrorCode = Integer.parseInt(errorCodeString);

                if (!ErrorPage.supportsErrorCode(desiredErrorCode)) {
                    // I don't think there's any good way of showing an error if there's an error
                    // in requesting an error page?
                    desiredErrorCode = WebViewClient.ERROR_BAD_URL;
                }
            } catch (final NumberFormatException e) {
                desiredErrorCode = WebViewClient.ERROR_BAD_URL;
            }
            if (this.errorPageDelegate != null) {
                this.errorPageDelegate.onReceivedError(webView, desiredErrorCode, description, failingUrl);
            }
            return;
        }


        // The API 23+ version also return a *slightly* more usable description, via WebResourceError.getError();
        // e.g.. "There was a network error.", whereas this version provides things like "net::ERR_NAME_NOT_RESOLVED"
        if (failingUrl.equals(currentPageURL) &&
                ErrorPage.supportsErrorCode(errorCode)) {
            if (this.errorPageDelegate != null) {
                this.errorPageDelegate.onReceivedError(webView, errorCode, description, failingUrl);
            }
            return;
        }

        super.onReceivedError(webView, errorCode, description, failingUrl);
    }

    @Override
    public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
        super.onReceivedHttpError(view, request, errorResponse);

        Uri url = request.getUrl();
        if (url != null) {
            this.debugOverlay.recordLifecycle("onReceivedHttpError:" + url.toString(), false);

            if (request.isForMainFrame() && TextUtils.equals(currentPageURL, url.toString())) {
                if (this.errorPageDelegate != null) {
                    this.errorPageDelegate.onReceivedHttpError(view, request, errorResponse);
                }
            }
        }
    }

    @Override
    public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
        super.doUpdateVisitedHistory(view, url, isReload);
        this.debugOverlay.updateHistory();
    }

    final void setDebugOverlay(@NonNull WebViewDebugOverlay overlay) {
        this.debugOverlay = overlay;
    }

    public void setErrorPageDelegate(ErrorPageDelegate errorPageDelegate) {
        this.errorPageDelegate = errorPageDelegate;
    }
}
