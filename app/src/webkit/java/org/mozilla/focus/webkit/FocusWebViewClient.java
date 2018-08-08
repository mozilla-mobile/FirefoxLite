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
import android.support.v7.app.AppCompatDelegate;
import android.text.TextUtils;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.mozilla.focus.R;
import org.mozilla.focus.utils.AppConstants;
import org.mozilla.focus.utils.Settings;
import org.mozilla.focus.utils.SupportUtils;
import org.mozilla.focus.utils.UrlUtils;
import org.mozilla.rocket.tabs.TabViewClient;

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
    private int nightThemeBackgroundColor;
    FocusWebViewClient(Context context) {
        super(context);
        nightThemeBackgroundColor = context.getResources().getColor(R.color.browserFragmentBackground);
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
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            view.setVisibility(View.INVISIBLE);
        }

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
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            view.setBackgroundColor(nightThemeBackgroundColor);
            String js = "javascript: ("
                    + "function () { "
                    + "var css = 'html {-webkit-filter: hue-rotate(180deg) invert(100%) !important;}'+"
                    + "          'html {background:#222222 !important;}'+"
                    + "          'img,video {-webkit-filter: brightness(80%) invert(100%) hue-rotate(180deg) !important;}',"
                    + "head = document.getElementsByTagName('head')[0],"
                    + "style = document.createElement('style');"
                    + "style.type = 'text/css';"
                    + "if (style.styleSheet){"
                    + "style.styleSheet.cssText = css;"
                    + "} else {"
                    + "style.appendChild(document.createTextNode(css));"
                    + "}"
                    //injecting the css to the head
                    + "head.appendChild(style);"
                    + "}());";

            //view.evaluateJavascript("(function() { css = document.createElement('link'); css.id = 'moz_rocket'; css.rel = 'stylesheet'; css.href = 'data:text/css,html,link,textarea,select,button,menu,aside,img,strong, body,header,div,a,span,table,tr,td,th,tbody,p,form,input,ul,ol,li,dl,dt,dd,section,footer,nav,h1,h2,h3,h4,h5,h6,em,pre{background: #333 !important;color:#616161!important;border-color:#454530!important;text-shadow:0!important;-webkit-text-fill-color : none!important;}html a,html a *{color:#5a8498!important;text-decoration:underline!important;}html a:visited,html a:visited *,html a:active,html a:active *{color:#505f64!important;}html a:hover,html a:hover *{color:#cef!important;}html input,html select,html button,html textarea{background:#4d4c40!important;border:1px solid #5c5a46!important;border-top-color:#494533!important;border-bottom-color:#494533!important;}html input[type=button],html input[type=submit],html input[type=reset],html input[type=image],html button{border-top-color:#494533!important;border-bottom-color:#494533!important;}html input:focus,html select:focus,html option:focus,html button:focus,html textarea:focus{background:#5c5b3e!important;color:#fff!important;border-color:#494100 #635d00 #474531!important;outline:1px solid #041d29!important;}html input[type=button]:focus,html input[type=submit]:focus,html input[type=reset]:focus,html input[type=image]:focus,html button:focus{border-color:#494533 #635d00 #474100!important;}html input[type=radio]{background:none!important;border-color:#333!important;border-width:0!important;}html img[src],html input[type=image]{opacity:.5;}html img[src]:hover,html input[type=image]:hover{opacity:1;}html,html body {scrollbar-base-color: #4d4c40 !important;scrollbar-face-color: #5a5b3c !important;scrollbar-shadow-color: #5a5b3c !important;scrollbar-highlight-color: #5c5b3c !important;scrollbar-dlight-color: #5c5b3c !important;scrollbar-darkshadow-color: #474531 !important;scrollbar-track-color: #4d4c40 !important;scrollbar-arrow-color: #000 !important;scrollbar-3dlight-color: #6a6957 !important;}dt a{background-color: #333 !important;}'; document.getElementsByTagName('head')[0].appendChild(css);})(); ",null);
            view.evaluateJavascript(js, null);
            view.setVisibility(View.VISIBLE);
        }

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
