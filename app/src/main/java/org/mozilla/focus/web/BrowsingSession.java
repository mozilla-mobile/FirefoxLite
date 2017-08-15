/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.web;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.mozilla.focus.utils.SafeIntent;

import java.lang.ref.WeakReference;

/**
 * A global object keeping the state of the current browsing session.
 * <p>
 * For now it only tracks whether a browsing session is active or not.
 */
public class BrowsingSession {
    private static BrowsingSession instance;

    private final static boolean ENABLE_CUSTOM_TABS = false;

    public interface TrackingCountListener {
        void onTrackingCountChanged(int trackingCount);
    }

    public static synchronized BrowsingSession getInstance() {
        if (instance == null) {
            instance = new BrowsingSession();
        }
        return instance;
    }

    private int blockedTrackers;
    private WeakReference<TrackingCountListener> listenerWeakReference;

    @Nullable
    private CustomTabConfig customTabConfig;

    private BrowsingSession() {
        listenerWeakReference = new WeakReference<>(null);
    }

    public void clearCustomTabConfig() {
        customTabConfig = null;
    }

    public void countBlockedTracker() {
        blockedTrackers++;

        final TrackingCountListener listener = listenerWeakReference.get();
        if (listener != null) {
            listener.onTrackingCountChanged(blockedTrackers);
        }
    }

    public void setTrackingCountListener(TrackingCountListener listener) {
        listenerWeakReference = new WeakReference<>(listener);
        listener.onTrackingCountChanged(blockedTrackers);
    }

    public void resetTrackerCount() {
        blockedTrackers = 0;
    }

    public void loadCustomTabConfig(final @NonNull Context context, final @NonNull SafeIntent intent) {
        if (ENABLE_CUSTOM_TABS && CustomTabConfig.isCustomTabIntent(intent)) {
            customTabConfig = CustomTabConfig.parseCustomTabIntent(context, intent);
        } else {
            customTabConfig = null;
        }
    }

    public boolean isCustomTab() {
        //noinspection ConstantConditions
        return (ENABLE_CUSTOM_TABS && customTabConfig != null);
    }

    @NonNull
    public CustomTabConfig getCustomTabConfig() {
        if (!isCustomTab()) {
            throw new IllegalStateException("Can't retrieve custom tab config for normal browsing session");
        }

        return customTabConfig;
    }
}
