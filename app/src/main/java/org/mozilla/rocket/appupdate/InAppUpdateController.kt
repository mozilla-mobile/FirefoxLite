/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.appupdate

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import org.mozilla.focus.BuildConfig
import org.mozilla.focus.activity.MainActivity
import org.mozilla.focus.utils.AppConfigWrapper
import org.mozilla.focus.utils.AppConstants
import org.mozilla.focus.utils.Settings

class InAppUpdateController(appContext: Context) : InAppUpdateManager(
    object : InAppUpdateModel {
        override fun isInAppUpdateLogEnabled(): Boolean {
            return AppConstants.isNightlyBuild() || BuildConfig.DEBUG
        }

        override fun getLastPromptInAppUpdateVersion(): Int {
            return Settings.getInstance(appContext).lastPromptInAppUpdateVersion
        }

        override fun setLastPromptInAppUpdateVersion(version: Int) {
            Settings.getInstance(appContext).lastPromptInAppUpdateVersion = version
        }

        override fun getInAppUpdateConfig(): InAppUpdateConfig? {
            return AppConfigWrapper.getInAppUpdateConfig()
        }

        override fun isAvailableToShowUpdateUi(): Boolean {
            return !Settings.getInstance(appContext).shouldShowFirstrun()
        }
    }
) {
    fun showIntroDialog(data: InAppUpdateData, delegate: InAppUpdateViewDelegate) {
        val intro = data.config.introData ?: return
        delegate.showIntroDialog({
            onIntroAgreed(data)
        }, {
            onIntroDenied(data)
        }, intro)
    }

    fun showInstallPrompt(delegate: InAppUpdateViewDelegate) {
        delegate.showInstallPrompt {
            onInstallAgreed()
        }
    }

    fun showDownloadStartHint(delegate: InAppUpdateViewDelegate) {
        delegate.showDownloadStartHint()
    }

    fun startUpdate(activity: Activity, manager: AppUpdateManager, data: InAppUpdateData) {
        manager.registerListener(createUpdateListener(manager))
        manager.startUpdateFlowForResult(
                data.info,
                AppUpdateType.FLEXIBLE,
                activity,
                MainActivity.REQUEST_CODE_IN_APP_UPDATE
        )
    }

    fun startInstall(manager: AppUpdateManager) {
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() != InstallStatus.DOWNLOADED) {
                log("no downloaded update, skip install")
                return@addOnSuccessListener
            }

            manager.completeUpdate().addOnSuccessListener {
                onInstallSuccess()
            }.addOnFailureListener {
                onInstallFailed()
            }.addOnCompleteListener {
                onInstallComplete()
            }
        }
    }

    private fun createUpdateListener(manager: AppUpdateManager): InstallStateUpdatedListener {
        return object : InstallStateUpdatedListener {
            override fun onStateUpdate(state: InstallState) {
                when (state.installStatus()) {
                    InstallStatus.DOWNLOADED -> {
                        log("download complete")
                        manager.unregisterListener(this)
                        onUpdateDownloaded()
                    }
                    InstallStatus.FAILED -> {
                        log("download failed")
                        manager.unregisterListener(this)
                        onUpdateDownloadFailed()
                    }
                    InstallStatus.CANCELED -> {
                        log("download cancelled")
                        manager.unregisterListener(this)
                        onUpdateDownloadCancelled()
                    }
                    else -> {
                        log("unhandled install status: ${state.installStatus()}")
                    }
                }
            }
        }
    }

    private fun log(msg: String) {
        if (model.isInAppUpdateLogEnabled()) {
            Log.d(TAG, msg)
        }
    }

    companion object {
        private const val TAG = "InAppUpdateController"
    }
}
