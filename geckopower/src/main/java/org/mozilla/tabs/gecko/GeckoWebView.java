package org.mozilla.tabs.gecko;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

import org.mozilla.focus.tabs.SiteIdentity;
import org.mozilla.focus.tabs.TabChromeClient;
import org.mozilla.focus.tabs.TabView;
import org.mozilla.focus.tabs.TabViewClient;
import org.mozilla.focus.web.DownloadCallback;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.geckoview.GeckoView;

public class GeckoWebView implements TabView {
    private String currentUrl = "about:blank";

    private GeckoView geckoView;
    private GeckoSessionSettings settings;
    private GeckoRuntime runtime;
    private GeckoWebClient client;

    GeckoSession session;

    public GeckoWebView(GeckoView gv, GeckoRuntime runtime) {
        this.geckoView = gv;
        this.settings = new GeckoSessionSettings();
        this.settings.setBoolean(GeckoSessionSettings.USE_MULTIPROCESS, false);
        this.session = new GeckoSession(settings);
        this.runtime = runtime;

        this.client = new GeckoWebClient(this);
        geckoView.releaseSession();
        geckoView.setSession(session, runtime);
    }

    public void onDetach() {
        this.session.setActive(false);
        this.geckoView.releaseSession();
    }

    public void onAttach(ViewGroup parent) {
        this.session.setActive(true);
        if (!session.isOpen()) {
            session.open(this.runtime);
        }
        this.geckoView.releaseSession();
        this.geckoView.setSession(this.session, this.runtime);
    }

    @Override
    public void onPause() {
        this.session.setActive(false);
    }

    @Override
    public void onResume() {
        this.session.setActive(true);
    }

    @Override
    public void destroy() {
    }

    @Override
    public void reload() {
    }

    @Override
    public void stopLoading() {
        // TODO
    }

    @Override
    public String getUrl() {
        return currentUrl;
    }

    @Override
    public String getTitle() {
        return null;
    }

    @Override
    public int getSecurityState() {
        return client.getSecurity() ? SiteIdentity.SECURE : SiteIdentity.INSECURE;
    }

    @Override
    public void loadUrl(final String url) {
        currentUrl = url;
        session.loadUri(currentUrl);
    }

    @Override
    public void cleanup() {
        // We're running in a private browsing window, so nothing to do
    }

    @Override
    public void goForward() {
        session.goForward();
    }

    @Override
    public void goBack() {
        session.goBack();
    }

    @Override
    public void setContentBlockingEnabled(boolean enabled) {
        // TODO: need implementation
    }

    @Override
    public void bindOnNewWindowCreation(@NonNull Message msg) {
        // TODO: need implementation
    }

    @Override
    public void setImageBlockingEnabled(boolean enabled) {
        // TODO: need implementation
    }

    @Override
    public boolean isBlockingEnabled() {
        return true;
    }

    @Override
    public void performExitFullScreen() {
        // TODO: need implementation
    }

    @Override
    public void setViewClient(@Nullable TabViewClient viewClient) {
        this.client.setViewClient(viewClient);
    }

    @Override
    public void setChromeClient(@Nullable TabChromeClient chromeClient) {
        this.client.setChromeClient(chromeClient);
    }

    @Override
    public void setDownloadCallback(DownloadCallback callback) {
        // TODO: need implementation
    }

    @Override
    public void restoreViewState(Bundle savedInstanceState) {
        // TODO: need implementation
    }

    @Override
    public void saveViewState(Bundle outState) {
        // TODO: need implementation
    }

    @Override
    public void insertBrowsingHistory() {
        // TODO: need implementation
    }

    @Override
    public View getView() {
        return geckoView;
    }

    @Override
    public void buildDrawingCache(boolean autoScale) {
        this.geckoView.buildDrawingCache(autoScale);
    }

    @Override
    public Bitmap getDrawingCache(boolean autoScale) {
        return this.geckoView.getDrawingCache(autoScale);
    }

    @Override
    public boolean canGoForward() {
        return client.canGoForward();
    }

    @Override
    public boolean canGoBack() {
        return client.canGoBack();
    }
}
