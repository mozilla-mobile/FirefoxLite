package org.mozilla.tabs.gecko;

import android.support.annotation.NonNull;

import org.mozilla.focus.tabs.TabChromeClient;
import org.mozilla.focus.tabs.TabViewClient;
import org.mozilla.geckoview.GeckoResponse;
import org.mozilla.geckoview.GeckoSession;

public class GeckoWebClient implements GeckoSession.ProgressDelegate,
        GeckoSession.NavigationDelegate,
        GeckoSession.ContentDelegate {

    private GeckoWebView hostView;
    private GeckoSession hostSession;

    private TabChromeClient tabChromeClient;
    private TabViewClient tabViewClient;
    private boolean isSecure = false;
    private boolean canGoBack = false;
    private boolean canGoForward = false;

    GeckoWebClient(@NonNull GeckoWebView view) {
        this.hostView = view;
        this.hostSession = view.session;
        this.hostSession.setProgressDelegate(this);
        this.hostSession.setNavigationDelegate(this);
        this.hostSession.setContentDelegate(this);
        // TODO: set long press listener, call through to callback.onLinkLongPress()
    }

    public void setChromeClient(TabChromeClient callback) {
        this.tabChromeClient = callback;
    }

    public void setViewClient(TabViewClient callback) {
        this.tabViewClient = callback;
    }

    public boolean canGoForward() {
        return canGoForward;
    }

    public boolean canGoBack() {
        return canGoBack;
    }

    public boolean getSecurity() {
        return isSecure;
    }

    /* GeckoView.ProgressDelegate */
    @Override
    public void onPageStart(GeckoSession session, String url) {
        this.isSecure = false;
        this.tabViewClient.onPageStarted(url);
        this.tabChromeClient.onProgressChanged(25);
    }

    @Override
    public void onPageStop(GeckoSession session, boolean success) {
        if (success) {
            this.tabChromeClient.onProgressChanged(100);
            this.tabViewClient.onPageFinished(isSecure);
        } else {
            this.tabChromeClient.onProgressChanged(0);
        }
    }

    @Override
    public void onSecurityChange(GeckoSession session,
                                 GeckoSession.ProgressDelegate.SecurityInformation info) {

        isSecure = info.isSecure;
    }

    /* GeckoView.NavigationDelegate */
    @Override
    public void onLocationChange(GeckoSession session, String url) {
        this.tabViewClient.onURLChanged(url);
    }

    @Override
    public void onCanGoBack(GeckoSession session, boolean canGoBack) {
        this.canGoBack = canGoBack;
    }

    @Override
    public void onCanGoForward(GeckoSession session, boolean canGoForward) {
        this.canGoForward = canGoForward;
    }

    @Override
    public void onLoadRequest(GeckoSession session, String uri, int target, int flags, GeckoResponse<Boolean> response) {
        // TODO: of course this implementation is buggy
        response.respond(false);
    }

    @Override
    public void onNewSession(GeckoSession session, String uri, GeckoResponse<GeckoSession> response) {

    }

    /* GeckoView.ContentDelegate*/
    @Override
    public void onTitleChange(GeckoSession session, String title) {
        this.tabChromeClient.onReceivedTitle(this.hostView, title);
    }

    @Override
    public void onFocusRequest(GeckoSession session) {
    }

    @Override
    public void onCloseRequest(GeckoSession session) {
    }

    @Override
    public void onFullScreen(GeckoSession session, boolean fullScreen) {
        //TODO: need implementation
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public void onContextMenu(GeckoSession session, int screenX, int screenY, String uri, int elementType, String elementSrc) {
    }

    @Override
    public void onExternalResponse(GeckoSession session, GeckoSession.WebResponseInfo response) {
    }
}
