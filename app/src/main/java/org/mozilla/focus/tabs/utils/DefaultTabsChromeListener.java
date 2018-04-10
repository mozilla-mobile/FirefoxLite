/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.tabs.utils;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;

import org.mozilla.focus.tabs.Tab;
import org.mozilla.focus.tabs.TabView;
import org.mozilla.focus.tabs.TabsChromeListener;

/**
 * A sugar class implements TabsChromeListener in empty implementation.
 */
public class DefaultTabsChromeListener implements TabsChromeListener {
    @Override
    public void onProgressChanged(@NonNull Tab tab, int progress) {

    }

    @Override
    public void onReceivedTitle(@NonNull Tab tab, String title) {

    }

    @Override
    public void onReceivedIcon(@NonNull Tab tab, Bitmap icon) {

    }

    @Override
    public void onFocusChanged(@Nullable Tab tab, int factor) {

    }

    @Override
    public void onTabAdded(@NonNull Tab tab, @Nullable Bundle arguments) {

    }

    @Override
    public void onTabCountChanged(int count) {

    }

    @Override
    public void onLongPress(@NonNull Tab tab, TabView.HitTarget hitTarget) {

    }

    @Override
    public void onEnterFullScreen(@NonNull Tab tab, @NonNull TabView.FullscreenCallback callback, @Nullable View fullscreenContent) {

    }

    @Override
    public void onExitFullScreen(@NonNull Tab tab) {

    }

    @Override
    public boolean onShowFileChooser(@NonNull Tab tab, TabView tabView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
        return false;
    }

    @Override
    public void onGeolocationPermissionsShowPrompt(@NonNull Tab tab, String origin, GeolocationPermissions.Callback callback) {

    }
}
