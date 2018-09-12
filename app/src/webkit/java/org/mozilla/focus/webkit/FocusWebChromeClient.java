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
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.utils.DimenUtils;
import org.mozilla.focus.utils.FileUtils;
import org.mozilla.icon.FavIconUtils;
import org.mozilla.rocket.tabs.TabChromeClient;
import org.mozilla.rocket.tabs.TabView;
import org.mozilla.rocket.util.Logger;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutionException;

/**
 * An @see{android.webkit.WebChromeClient} implementation to hand over any callback to TabChromeClient, if any.
 */
class FocusWebChromeClient extends WebChromeClient {

    private static final String LOGGER_TAG = "FocusWebChromeClient";

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
            this.tabChromeClient.onCloseWindow(this.host);
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
        // We're desperate in finding the correct callback for updating title, so also updating here.
        final String title = view.getTitle();

        try {
            new FavIconUtils.SaveBitmapTask(new FileUtils.GetCache(
                    new WeakReference<>(view.getContext())).get(), url, icon,
                    new BrowsingHistoryManager.UpdateHistoryWrapper(title, url),
                    Bitmap.CompressFormat.PNG, DimenUtils.PNG_QUALITY_DONT_CARE).execute();
        } catch (ExecutionException | InterruptedException e) {
            Logger.throwOrWarn(LOGGER_TAG, "Failed to get cache folder in onReceivedIcon.");
        }

        if (this.tabChromeClient != null) {
            this.tabChromeClient.onReceivedIcon(this.host, icon);
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
                && tabChromeClient.onShowFileChooser(this.host, filePathCallback, fileChooserParams);
    }

    @Override
    public void onReceivedTitle(WebView view, String title) {
        super.onReceivedTitle(view, title);
        if (tabChromeClient != null) {
            tabChromeClient.onReceivedTitle(this.host, title);
        }
    }
}
