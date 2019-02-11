/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.helper;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.test.espresso.IdlingResource;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mozilla.rocket.tabs.Session;
import org.mozilla.rocket.tabs.SessionManager;
import org.mozilla.rocket.tabs.TabView;
import org.mozilla.rocket.tabs.TabViewEngineSession;
import org.mozilla.rocket.tabs.TabsSessionProvider;

import mozilla.components.browser.session.Download;


/**
 * An IdlingResource implementation that waits until the BrowserFragment is not loading anymore.
 * Only after loading has completed further actions will be performed.
 */
public class SessionLoadedIdlingResource implements IdlingResource {
    private ResourceCallback resourceCallback;
    private boolean isCompleted;
    private Observer observer = new Observer();

    public SessionLoadedIdlingResource(TabsSessionProvider.SessionHost activity) {
        final SessionManager sessionManager = activity.getSessionManager();
        sessionManager.register(observer);
    }

    @Override
    public String getName() {
        return SessionLoadedIdlingResource.class.getSimpleName();
    }

    @Override
    public boolean isIdleNow() {
        return isCompleted;
    }

    private void invokeCallback() {
        if (resourceCallback != null) {
            resourceCallback.onTransitionToIdle();
        }
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback callback) {
        this.resourceCallback = callback;
    }


    class Observer implements SessionManager.Observer, Session.Observer {

        @Override
        public void updateFailingUrl(@Nullable String url, boolean updateFromError) {

        }

        @Override
        public boolean handleExternalUrl(@Nullable String url) {
            return false;
        }

        @Override
        public boolean onShowFileChooser(@NotNull TabViewEngineSession es, @Nullable ValueCallback<Uri[]> filePathCallback, @Nullable WebChromeClient.FileChooserParams fileChooserParams) {
            return false;
        }

        @Override
        public void onLoadingStateChanged(@NotNull Session session, boolean loading) {
            isCompleted = !loading;
            if (isCompleted) {
                invokeCallback();
            }
        }

        @Override
        public void onNavigationStateChanged(@NotNull Session session, boolean canGoBack, boolean canGoForward) {

        }

        @Override
        public void onSecurityChanged(@NotNull Session session, boolean isSecure) {

        }

        @Override
        public void onUrlChanged(@NotNull Session session, @Nullable String url) {

        }

        @Override
        public void onProgress(@NotNull Session session, int progress) {

        }

        @Override
        public void onTitleChanged(@NotNull Session session, @Nullable String title) {

        }

        @Override
        public void onReceivedIcon(@Nullable Bitmap icon) {

        }

        @Override
        public void onLongPress(@NotNull Session session, @NotNull TabView.HitTarget hitTarget) {

        }

        @Override
        public boolean onDownload(@NotNull Session session, @NotNull Download download) {
            return false;
        }

        @Override
        public void onFindResult(@NotNull Session session, @NotNull mozilla.components.browser.session.Session.FindResult result) {

        }

        @Override
        public void onEnterFullScreen(@NotNull TabView.FullscreenCallback callback, @Nullable View view) {

        }

        @Override
        public void onExitFullScreen() {

        }

        @Override
        public void onGeolocationPermissionsShowPrompt(@NotNull String origin, @Nullable GeolocationPermissions.Callback callback) {

        }

        @Override
        public void onFocusChanged(@Nullable Session session, @NotNull SessionManager.Factor factor) {
        }

        @Override
        public void onSessionAdded(@NotNull Session session, @Nullable Bundle arguments) {
            session.register(observer);
        }

        @Override
        public void onSessionCountChanged(int count) {
        }
    }

}
