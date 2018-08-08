/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus

import android.app.ActivityManager
import android.content.Context
import android.preference.PreferenceManager
import com.squareup.leakcanary.LeakCanary
import org.mozilla.focus.download.DownloadInfoManager
import org.mozilla.focus.history.BrowsingHistoryManager
import org.mozilla.focus.locale.LocaleAwareApplication
import org.mozilla.focus.notification.NotificationUtil
import org.mozilla.focus.screenshot.ScreenshotManager
import org.mozilla.focus.search.SearchEngineManager
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.AdjustHelper
import org.mozilla.rocket.privately.PrivateMode.Companion.PRIVATE_PROCESS_NAME
import org.mozilla.rocket.privately.PrivateMode.Companion.WEBVIEW_FOLDER_NAME
import java.io.File

class FocusApplication : LocaleAwareApplication() {

    override fun getCacheDir(): File {
        if (isInPrivateProcess()) {
            return File(super.getCacheDir().absolutePath + "-" + PRIVATE_PROCESS_NAME)
        }
        return super.getCacheDir()
    }

    override fun getDir(name: String?, mode: Int): File {
        if (name == WEBVIEW_FOLDER_NAME && isInPrivateProcess()) {
            return super.getDir("$name-$PRIVATE_PROCESS_NAME", mode)
        }
        return super.getDir(name, mode)
    }

    override fun onCreate() {
        super.onCreate()
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return
        }
        LeakCanary.install(this)

        PreferenceManager.setDefaultValues(this, R.xml.settings, false)

        // Provide different strict mode penalty for ui testing and production code
        Inject.enableStrictMode()

        SearchEngineManager.getInstance().init(this)

        TelemetryWrapper.init(this)
        AdjustHelper.setupAdjustIfNeeded(this)

        BrowsingHistoryManager.getInstance().init(this)
        ScreenshotManager.getInstance().init(this)
        DownloadInfoManager.getInstance()
        DownloadInfoManager.init(this)
        // initialize the NotificationUtil to configure the default notification channel. This is required for API 26+
        NotificationUtil.init(this)
    }

    private fun isInPrivateProcess(): Boolean {

        val pid = android.os.Process.myPid()
        val manager = this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (processInfo in manager.runningAppProcesses) {
            if (processInfo.pid == pid) {
                return processInfo.processName.contains(PRIVATE_PROCESS_NAME)
            }
        }
        return false
    }
}
