/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus;

import android.preference.PreferenceManager;

import com.squareup.leakcanary.AndroidExcludedRefs;
import com.squareup.leakcanary.DisplayLeakService;
import com.squareup.leakcanary.LeakCanary;

import org.mozilla.focus.download.DownloadInfoManager;
import org.mozilla.focus.history.BrowsingHistoryManager;
import org.mozilla.focus.locale.LocaleAwareApplication;
import org.mozilla.focus.screenshot.ScreenshotManager;
import org.mozilla.focus.search.SearchEngineManager;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.utils.AdjustHelper;
import org.mozilla.focus.utils.AndroidTestAnalysisResultService;

public class AndroidTestApplication extends FocusApplication {

    @Override
    protected void setupLeakCanary() {
        // We use AndroidTestAnalysisResultService to do extra work after a dump is analyzed.
        LeakCanary.refWatcher(this).listenerServiceClass(AndroidTestAnalysisResultService.class)
                .excludedRefs(AndroidExcludedRefs.createAppDefaults().build())
                .buildAndInstall();
    }

}
