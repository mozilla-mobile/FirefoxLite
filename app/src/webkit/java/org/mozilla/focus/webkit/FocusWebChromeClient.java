package org.mozilla.focus.webkit;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Message;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import org.mozilla.focus.history.BrowsingHistoryManager;
import org.mozilla.focus.history.model.Site;
import org.mozilla.focus.tabs.TabChromeClient;
import org.mozilla.focus.tabs.TabView;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.utils.FavIconUtils;

/**
 * An @see{android.webkit.WebChromeClient} implementation to hand over any callback to TabChromeClient, if any.
 */
class FocusWebChromeClient extends WebChromeClient {

    /**
     * The TabView be attached by this client. No matter which WebView notify this client, this client
     * always hand over notification to TabChromeClient with this hosted TabView.
     */
    private TabView host;

    private TabChromeClient tabChromeClient;

    FocusWebChromeClient(@NonNull TabView tabView) {
        this.host = tabView;
    }

    public void setChromeClient(TabChromeClient callback) {
        this.tabChromeClient = callback;
    }

    @Override
    public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message msg) {
        return (this.tabChromeClient != null)
                && this.tabChromeClient.onCreateWindow(isDialog, isUserGesture, msg);
    }

    @Override
    public void onCloseWindow(WebView view) {
        if (this.tabChromeClient != null) {
            this.tabChromeClient.onCloseWindow(view);
        }
    }

    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        if (this.tabChromeClient != null) {
            this.tabChromeClient.onProgressChanged(newProgress);
        }
    }

    @Override
    public void onReceivedIcon(WebView view, Bitmap icon) {
        final String url = view.getUrl();
        if (TextUtils.isEmpty(url)) {
            return;
        }

        final Bitmap refinedBitmap = FavIconUtils.getRefinedBitmap(view.getResources(),
                icon,
                FavIconUtils.getRepresentativeCharacter(url));

        final Site site = new Site();
        site.setTitle(view.getTitle());
        site.setUrl(url);
        site.setFavIcon(refinedBitmap);
        BrowsingHistoryManager.getInstance().updateLastEntry(site, null);

        if (this.tabChromeClient != null) {
            this.tabChromeClient.onReceivedIcon(view, icon);
        }
    }

    @Override
    public void onShowCustomView(View view, final CustomViewCallback webviewCallback) {
        final TabView.FullscreenCallback fullscreenCallback = new TabView.FullscreenCallback() {
            @Override
            public void fullScreenExited() {
                webviewCallback.onCustomViewHidden();
            }
        };

        if (this.tabChromeClient != null) {
            this.tabChromeClient.onEnterFullScreen(fullscreenCallback, view);
        }
        TelemetryWrapper.browseEnterFullScreenEvent();
    }

    @Override
    public void onHideCustomView() {
        if (this.tabChromeClient != null) {
            this.tabChromeClient.onExitFullScreen();
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
        if (this.tabChromeClient != null) {
            this.tabChromeClient.onGeolocationPermissionsShowPrompt(origin, glpcallback);
        }
    }

    @Override
    public void onGeolocationPermissionsHidePrompt() {
        super.onGeolocationPermissionsHidePrompt();
    }

    @Override
    public boolean onShowFileChooser(WebView webView,
                                     ValueCallback<Uri[]> filePathCallback,
                                     FileChooserParams fileChooserParams) {

        return (this.tabChromeClient != null)
                && tabChromeClient.onShowFileChooser(webView, filePathCallback, fileChooserParams);
    }

    @Override
    public void onReceivedTitle(WebView view, String title) {
        super.onReceivedTitle(view, title);
        if (tabChromeClient != null) {
            tabChromeClient.onReceivedTitle(this.host, title);
        }
    }
}
