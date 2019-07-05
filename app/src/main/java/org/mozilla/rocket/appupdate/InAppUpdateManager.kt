/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.appupdate

import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import org.mozilla.focus.BuildConfig
import org.mozilla.rocket.download.SingleLiveEvent

data class InAppUpdateIntro(
    val title: String,
    val description: String,
    val positiveText: String,
    val negativeText: String
)

data class InAppUpdateConfig(
    val targetVersion: Int,
    val forceCloseOnDenied: Boolean,
    val showIntro: Boolean,
    val introData: InAppUpdateIntro? = null
)

fun InAppUpdateConfig.isUpdateRecommended(): Boolean {
    return targetVersion > BuildConfig.VERSION_CODE
}

fun InAppUpdateConfig.shouldShowIntro(): Boolean {
    return showIntro && introData != null
}

fun InAppUpdateConfig.getIntroIfAvailable(): InAppUpdateIntro? {
    return introData.takeIf { shouldShowIntro() }
}

class InAppUpdateManager(val model: InAppUpdateModel) {
    private var updateConfig = model.getInAppUpdateConfig()

    val startUpdate = SingleLiveEvent<InAppUpdateData>()
    val startInstall = SingleLiveEvent<Unit>()
    val startInstallExistingDownload = SingleLiveEvent<Unit>()
    val closeApp = SingleLiveEvent<Unit>()

    val showIntroDialog = SingleLiveEvent<InAppUpdateData>()
    val showInstallPrompt = SingleLiveEvent<InAppUpdateData>()
    val showInstallPromptForExistDownload = SingleLiveEvent<InAppUpdateData>()
    val showDownloadStartHint = SingleLiveEvent<Unit>()

    fun checkUpdate(info: AppUpdateInfo) {
        val config = updateConfig ?: return
        log("config: $config")

        if (!config.isUpdateRecommended()) {
            log("(current=${BuildConfig.VERSION_CODE} >= target=${config.targetVersion}), skip")
            return
        }

        log("install status: ${info.installStatus()}, availability: ${info.updateAvailability()}")

        val data = InAppUpdateData(info, config)
        if (hasDownloadedUpdate(info)) {
            log("detect a downloaded update")
            onExistingDownloadDetected(data)
            return
        }

        if (info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) {
            log("no update available")
            return
        }

        if (!info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
            log("flexible type update not allowed")
            return
        }

        log("flexible type update allowed")

        val currentVersion = BuildConfig.VERSION_CODE
        val configVersion = config.targetVersion
        val availableVersion = info.availableVersionCode()

        if (!config.forceCloseOnDenied && isVersionPromptBefore(availableVersion)) {
            log("version $availableVersion had been asked before, skip")
            return
        }
        log("first time prompt for version $availableVersion")
        model.setLastPromptInAppUpdateVersion(availableVersion)

        log("Firebase target version: $configVersion")
        log("will update from $currentVersion to $availableVersion")

        config.getIntroIfAvailable()?.let { _ ->
            showIntroDialog.value = data
        } ?: run {
            startGooglePlayUpdate(data)
        }
    }

    fun onIntroAgreed(data: InAppUpdateData) {
        startGooglePlayUpdate(data)
    }

    fun onIntroDenied(data: InAppUpdateData) {
        if (data.config.forceCloseOnDenied) {
            closeApp.call()
        }
    }

    fun onUpdateAgreed() {
        showDownloadStartHint.call()
    }

    fun onUpdateDenied() {
        val config = updateConfig ?: return

        val forceClose = !config.shouldShowIntro() && config.forceCloseOnDenied
        if (forceClose) {
            log("intro denied, force close")
            closeApp.call()
        } else {
            log("intro denied, skip")
        }
    }

    fun onInstallAgreed() {
        startInstall.call()
    }

    fun onInstallExistingDownloadAgreed() {
        startInstallExistingDownload.call()
    }

    fun onInstallFailed() { /* nothing to do for now */ }
    fun onInstallSuccess() { /* nothing to do for now */ }
    fun onInstallComplete() { /* nothing to do for now */ }

    fun onUpdateDownloadFailed() { /* nothing to do for now */ }
    fun onUpdateDownloadCancelled() { /* nothing to do for now */ }
    fun onUpdateDownloaded(data: InAppUpdateData) {
        showInstallPrompt.value = data
    }

    private fun onExistingDownloadDetected(data: InAppUpdateData) {
        showInstallPromptForExistDownload.value = data
    }

    private fun startGooglePlayUpdate(data: InAppUpdateData) {
        log("start google play update process")
        startUpdate.value = data
    }

    private fun isVersionPromptBefore(version: Int): Boolean {
        return model.getLastPromptInAppUpdateVersion() >= version
    }

    private fun hasDownloadedUpdate(info: AppUpdateInfo): Boolean {
        return info.installStatus() == InstallStatus.DOWNLOADED
    }

    private fun log(msg: String) {
        if (model.isInAppUpdateLogEnabled()) {
            Log.d(TAG, msg)
        }
    }

    interface InAppUpdateModel {
        fun isInAppUpdateLogEnabled(): Boolean
        fun getLastPromptInAppUpdateVersion(): Int
        fun setLastPromptInAppUpdateVersion(version: Int)
        fun getInAppUpdateConfig(): InAppUpdateConfig?
    }

    data class InAppUpdateData(val info: AppUpdateInfo, val config: InAppUpdateConfig)

    companion object {
        private const val TAG = "InAppUpdateManager"
    }
}
