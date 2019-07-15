/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.helper;

import android.graphics.Bitmap;

import androidx.test.espresso.IdlingResource;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import mozilla.components.browser.session.Download;
import mozilla.components.browser.session.Session;
import mozilla.components.browser.session.SessionManager;
import mozilla.components.browser.session.manifest.WebAppManifest;
import mozilla.components.browser.session.tab.CustomTabConfig;
import mozilla.components.concept.engine.HitResult;
import mozilla.components.concept.engine.media.Media;
import mozilla.components.concept.engine.permission.PermissionRequest;
import mozilla.components.concept.engine.prompt.PromptRequest;
import mozilla.components.concept.engine.window.WindowRequest;


/**
 * An IdlingResource implementation that waits until the BrowserFragment is not loading anymore.
 * Only after loading has completed further actions will be performed.
 */
public class PrivateSessionLoadedIdlingResource implements IdlingResource {
    private String tag;
    private ResourceCallback resourceCallback;
    private boolean isCompleted;
    private Session lastSession;

    public PrivateSessionLoadedIdlingResource(SessionManager sessionManager, String tag) {
        Session selectedSession = sessionManager.getSelectedSession();
        this.tag = tag;
        if (selectedSession != null) {
            selectedSession.register(sessionObserver);
            lastSession = selectedSession;
        }
        sessionManager.register(sessionManagerObserver);
    }

    @Override
    public String getName() {
        return PrivateSessionLoadedIdlingResource.class.getSimpleName() + ":" + tag;
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

    private SessionManager.Observer sessionManagerObserver = new SessionManager.Observer() {
        @Override
        public void onSessionSelected(@NotNull Session session) {
            Session lastSession = PrivateSessionLoadedIdlingResource.this.lastSession;
            if (lastSession != null) {
                lastSession.unregister(sessionObserver);
            }
            session.register(sessionObserver);
            PrivateSessionLoadedIdlingResource.this.lastSession = session;
        }

        @Override
        public void onSessionAdded(@NotNull Session session) {

        }

        @Override
        public void onSessionsRestored() {

        }

        @Override
        public void onSessionRemoved(@NotNull Session session) {

        }

        @Override
        public void onAllSessionsRemoved() {

        }
    };

    private Session.Observer sessionObserver = new Session.Observer() {
        @Override
        public void onWebAppManifestChanged(@NotNull Session session, @Nullable WebAppManifest webAppManifest) {

        }

        @Override
        public void onUrlChanged(@NotNull Session session, @NotNull String s) {

        }

        @Override
        public void onTrackerBlockingEnabledChanged(@NotNull Session session, boolean b) {

        }

        @Override
        public void onTrackerBlocked(@NotNull Session session, @NotNull String s, @NotNull List<String> list) {

        }

        @Override
        public void onTitleChanged(@NotNull Session session, @NotNull String s) {

        }

        @Override
        public void onThumbnailChanged(@NotNull Session session, @Nullable Bitmap bitmap) {

        }

        @Override
        public void onSecurityChanged(@NotNull Session session, @NotNull Session.SecurityInfo securityInfo) {

        }

        @Override
        public void onSearch(@NotNull Session session, @NotNull String s) {

        }

        @Override
        public void onReaderableStateUpdated(@NotNull Session session, boolean b) {

        }

        @Override
        public void onReaderModeChanged(@NotNull Session session, boolean b) {

        }

        @Override
        public boolean onPromptRequested(@NotNull Session session, @NotNull PromptRequest promptRequest) {
            return false;
        }

        @Override
        public void onProgress(@NotNull Session session, int i) {

        }

        @Override
        public boolean onOpenWindowRequested(@NotNull Session session, @NotNull WindowRequest windowRequest) {
            return false;
        }

        @Override
        public void onNavigationStateChanged(@NotNull Session session, boolean b, boolean b1) {

        }

        @Override
        public void onMediaRemoved(@NotNull Session session, @NotNull List<? extends Media> list, @NotNull Media media) {

        }

        @Override
        public void onMediaAdded(@NotNull Session session, @NotNull List<? extends Media> list, @NotNull Media media) {

        }

        @Override
        public boolean onLongPress(@NotNull Session session, @NotNull HitResult hitResult) {
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
        public void onIconChanged(@NotNull Session session, @Nullable Bitmap bitmap) {

        }

        @Override
        public void onFullScreenChanged(@NotNull Session session, boolean b) {

        }

        @Override
        public void onFindResult(@NotNull Session session, @NotNull Session.FindResult findResult) {

        }

        @Override
        public boolean onDownload(@NotNull Session session, @NotNull Download download) {
            return false;
        }

        @Override
        public void onDesktopModeChanged(@NotNull Session session, boolean b) {

        }

        @Override
        public void onCustomTabConfigChanged(@NotNull Session session, @Nullable CustomTabConfig customTabConfig) {

        }

        @Override
        public void onCrashStateChanged(@NotNull Session session, boolean b) {

        }

        @Override
        public boolean onContentPermissionRequested(@NotNull Session session, @NotNull PermissionRequest permissionRequest) {
            return false;
        }

        @Override
        public boolean onCloseWindowRequested(@NotNull Session session, @NotNull WindowRequest windowRequest) {
            return false;
        }

        @Override
        public boolean onAppPermissionRequested(@NotNull Session session, @NotNull PermissionRequest permissionRequest) {
            return false;
        }
    };
}
