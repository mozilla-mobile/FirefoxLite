/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.tabs.utils;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;

import org.mozilla.rocket.tabs.Session;
import org.mozilla.rocket.tabs.TabView;
import org.mozilla.rocket.tabs.TabsChromeListener;

/**
 * A sugar class implements TabsChromeListener in empty implementation.
 */
public class DefaultTabsChromeListener implements TabsChromeListener {
    @Override
    public void onProgressChanged(@NonNull Session tab, int progress) {
    }

    @Override
    public void onReceivedTitle(@NonNull Session tab, String title) {
    }

    @Override
    public void onReceivedIcon(@NonNull Session tab, Bitmap icon) {
    }

    @Override
    public void onFocusChanged(@Nullable Session tab, int factor) {
    }

    @Override
    public void onTabAdded(@NonNull Session tab, @Nullable Bundle arguments) {
    }

    @Override
    public void onTabCountChanged(int count) {
    }

    @Override
    public void onLongPress(@NonNull Session tab, TabView.HitTarget hitTarget) {
    }

    @Override
    public void onEnterFullScreen(@NonNull Session tab, @NonNull TabView.FullscreenCallback callback, @Nullable View fullscreenContent) {
    }

    @Override
    public void onExitFullScreen(@NonNull Session tab) {
    }

    @Override
    public boolean onShowFileChooser(@NonNull Session tab, TabView tabView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
        return false;
    }

    @Override
    public void onGeolocationPermissionsShowPrompt(@NonNull Session tab, String origin, GeolocationPermissions.Callback callback) {
    }
}
