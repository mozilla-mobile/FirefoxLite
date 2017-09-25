/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.locale;

import android.app.Application;
import android.content.res.Configuration;

public class LocaleAwareApplication extends Application {
    private boolean mInBackground;

    @Override
    public void onCreate() {
        Locales.initializeLocale(this);

        super.onCreate();
    }

    /**
     * We need to do locale work here, because we need to intercept
     * each hit to onConfigurationChanged.
     */
    @Override
    public void onConfigurationChanged(Configuration config) {
        // Do nothing if we're in the background. It'll simply cause a loop
        // (Bug 936756 Comment 11), and it's not necessary.
        if (mInBackground) {
            super.onConfigurationChanged(config);
            return;
        }

        // Otherwise, correct the locale. This catches some cases that the current Activity
        // doesn't get a chance to.
        try {
            LocaleManager.getInstance().correctLocale(this, getResources(), config);
        } catch (IllegalStateException ex) {
            // Activity hasn't started yey, so we have no ContextGetter in LocaleManager.
        }

        super.onConfigurationChanged(config);
    }


    public void onActivityPause() {
        mInBackground = true;
    }

    public void onActivityResume() {
        mInBackground = true;
    }
}
