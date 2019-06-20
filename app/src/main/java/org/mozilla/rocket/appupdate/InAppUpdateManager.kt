/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.appupdate

import android.app.Activity
import android.arch.lifecycle.Lifecycle
import android.content.Context
import android.support.v4.app.FragmentActivity
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import org.mozilla.focus.BuildConfig

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
    return if (shouldShowIntro()) { introData } else { null }
}

class InAppUpdateManager(
    private val delegate: InAppUpdateUIDelegate,
    private val model: InAppUpdateModel
) {
    private var updateConfig: InAppUpdateConfig? = null

    fun update(activity: FragmentActivity, config: InAppUpdateConfig?) {
        updateConfig = config ?: return

        val updateManager = AppUpdateManagerFactory.create(activity)
        updateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (!activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                log("ui is not visible, skip")
                return@addOnSuccessListener
            }

            log("install status: ${info.installStatus()}, availability: ${info.updateAvailability()}")

            if (hasDownloadedUpdate(info)) {
                log("detect a downloaded update")
                delegate.showInAppUpdateInstallPrompt { installUpdate(updateManager) }
                return@addOnSuccessListener
            }

            if (info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) {
                log("no update available")
                return@addOnSuccessListener
            }

            if (info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                log("flexible type update allowed")
                startInAppUpdate(activity, updateManager, info, config)
            } else {
                log("flexible type update not allowed")
            }
        }
    }

    fun onInAppUpdateDenied() {
        val config = updateConfig ?: return

        log("google play update dialog denied")
        val forceClose = !config.shouldShowIntro() && config.forceCloseOnDenied
        if (forceClose) {
            log("google play update dialog denied, force close")
            delegate.closeOnInAppUpdateDenied()
        } else {
            log("google play update dialog denied, skip")
        }
    }

    fun onInAppUpdateGranted() {
        log("google play update dialog granted")
        delegate.showInAppUpdateDownloadStartHint()
    }

    fun installUpdate(context: Context) {
        installUpdate(AppUpdateManagerFactory.create(context))
    }

    private fun startInAppUpdate(
        activity: Activity,
        manager: AppUpdateManager,
        info: AppUpdateInfo,
        config: InAppUpdateConfig
    ) {
        log("config: $config")

        if (!config.isUpdateRecommended()) {
            log("no urgent update is needed, abort")
            return
        }

        val currentVersion = BuildConfig.VERSION_CODE
        val configVersion = config.targetVersion
        val availableVersion = info.availableVersionCode()

        if (isVersionPromptBefore(availableVersion)) {
            log("version $availableVersion had been asked before, skip")
            return
        } else {
            log("first time prompt for version $availableVersion")
            model.setLastPromptInAppUpdateVersion(availableVersion)
        }

        log("Firebase target version: $configVersion")
        log("will update from $currentVersion to $availableVersion")

        config.getIntroIfAvailable()?.let { introData ->
            showIntro(activity, manager, info, config.forceCloseOnDenied, introData)
        } ?: run {
            startGooglePlayUpdate(activity, manager, info)
        }
    }

    private fun showIntro(
        activity: Activity,
        manager: AppUpdateManager,
        info: AppUpdateInfo,
        forceCloseOnDenied: Boolean,
        introData: InAppUpdateIntro
    ) {
        delegate.showInAppUpdateIntro(object : InAppUpdateIntroCallback {
            override fun onPositive() {
                startGooglePlayUpdate(activity, manager, info)
            }

            override fun onNegative() {
                if (forceCloseOnDenied) {
                    log("intro denied, force close")
                    delegate.closeOnInAppUpdateDenied()
                } else {
                    log("intro denied, skip")
                }
            }
        }, introData)
    }

    private fun installUpdate(manager: AppUpdateManager) {
        log("start install update")
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                manager.completeUpdate().addOnSuccessListener {
                    log("install success")
                }.addOnFailureListener {
                    log("install failed")
                }.addOnCompleteListener {
                    log("install complete")
                }
            }
        }
    }

    private fun startGooglePlayUpdate(
        activity: Activity,
        manager: AppUpdateManager,
        info: AppUpdateInfo
    ) {
        log("start google play update process")
        manager.registerListener(createUpdateListener(manager))
        manager.startUpdateFlowForResult(
                info,
                AppUpdateType.FLEXIBLE,
                activity,
                model.getInAppUpdateRequestCode()
        )
    }

    private fun createUpdateListener(manager: AppUpdateManager): InstallStateUpdatedListener {
        return object : InstallStateUpdatedListener {
            override fun onStateUpdate(state: InstallState) {
                log("install status updated: ${state.installStatus()}")
                when (state.installStatus()) {
                    InstallStatus.DOWNLOADED -> {
                        log("install status: downloaded")
                        manager.unregisterListener(this)
                        delegate.showInAppUpdateInstallPrompt { installUpdate(manager) }
                    }
                    InstallStatus.FAILED -> {
                        log("install status: failed")
                        manager.unregisterListener(this)
                    }
                    InstallStatus.CANCELED -> {
                        log("install status: failed")
                        manager.unregisterListener(this)
                    }
                    else -> {
                        log("install status: ${state.installStatus()}")
                    }
                }
            }
        }
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

    interface InAppUpdateIntroCallback {
        fun onPositive()
        fun onNegative()
    }

    interface InAppUpdateUIDelegate {
        fun showInAppUpdateIntro(callback: InAppUpdateIntroCallback, data: InAppUpdateIntro): Boolean
        fun showInAppUpdateInstallPrompt(action: () -> Unit)
        fun showInAppUpdateDownloadStartHint()
        fun closeOnInAppUpdateDenied()
    }

    interface InAppUpdateModel {
        fun getInAppUpdateRequestCode(): Int
        fun isInAppUpdateLogEnabled(): Boolean
        fun getLastPromptInAppUpdateVersion(): Int
        fun setLastPromptInAppUpdateVersion(version: Int)
    }

    companion object {
        private const val TAG = "InAppUpdateManager"
    }
}
