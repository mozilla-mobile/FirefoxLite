/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.appupdate

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import org.mozilla.focus.BuildConfig
import org.mozilla.focus.activity.MainActivity
import org.mozilla.focus.activity.MainActivity.Companion.ACTION_INSTALL_IN_APP_UPDATE
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.AppConfigWrapper
import org.mozilla.focus.utils.AppConstants
import org.mozilla.focus.utils.NewFeatureNotice
import org.mozilla.focus.utils.Settings
import org.mozilla.rocket.extension.nonNullObserve

class InAppUpdateController(
    private val activity: FragmentActivity,
    private val appUpdateManager: AppUpdateManager,
    private val viewDelegate: ViewDelegate,
    private val inAppUpdateModel: InAppUpdateManager.InAppUpdateModel =
            object : InAppUpdateManager.InAppUpdateModel {
                override fun isInAppUpdateLogEnabled(): Boolean {
                    return AppConstants.isNightlyBuild() || BuildConfig.DEBUG
                }

                override fun getLastPromptInAppUpdateVersion(): Int {
                    return Settings.getInstance(activity.applicationContext).lastPromptInAppUpdateVersion
                }

                override fun setLastPromptInAppUpdateVersion(version: Int) {
                    Settings.getInstance(activity.applicationContext).lastPromptInAppUpdateVersion = version
                }

                override fun getInAppUpdateConfig(): InAppUpdateConfig? {
                    return AppConfigWrapper.getInAppUpdateConfig()
                }
            }
) {

    private val appContext = activity.applicationContext
    private var inAppUpdateManager = InAppUpdateManager(inAppUpdateModel)

    private var installFromIntent = false
    private var shouldShowFirstRun = NewFeatureNotice.getInstance(appContext).shouldShowLiteUpdate()
    private var isSideLoaded = try {
        appContext.packageManager.getInstallerPackageName(appContext.packageName)
    } catch (e: IllegalArgumentException) {
        ""
    }.isNullOrEmpty()

    private var pendingRequestData: InAppUpdateManager.InAppUpdateData? = null

    init {
        with(inAppUpdateManager) {
            startUpdate.nonNullObserve(activity) { startUpdate(activity, appUpdateManager, it) }
            startInstall.observe(activity, Observer {
                startInstall(appUpdateManager, isExistingDownload = false, isFromNotification = false)
            })
            startInstallExistingDownload.observe(activity, Observer {
                startInstall(appUpdateManager, isExistingDownload = true, isFromNotification = false)
            })
            closeApp.observe(activity, Observer { activity.finish() })

            showIntroDialog.nonNullObserve(activity) { showIntroDialog(it, viewDelegate) }
            showInstallPrompt.nonNullObserve(activity) { showInstallPrompt(it, viewDelegate) }
            showInstallPromptForExistDownload.nonNullObserve(activity) {
                showInstallPromptForExistDownload(it, viewDelegate)
            }
            showDownloadStartHint.observe(activity, Observer { showDownloadStartHint(viewDelegate) })
        }
    }

    fun checkUpdate() {
        if (isSideLoaded) {
            log("side-load install, skip")
            return
        }

        if (shouldShowFirstRun) {
            log("first run is about to show, skip")
            return
        }

        if (installFromIntent) {
            log("install is triggered by intent, skip")
            return
        }

        appUpdateManager.appUpdateInfo.addOnSuccessListener {
            inAppUpdateManager.checkUpdate(it)
        }
    }

    fun onReceiveIntent(intent: Intent?) {
        installFromIntent = isInAppUpdateInstallIntent(intent)
        if (installFromIntent) {
            startInstall(appUpdateManager, isExistingDownload = false, isFromNotification = true)
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int) {
        val requestData = pendingRequestData ?: return
        if (requestCode == MainActivity.REQUEST_CODE_IN_APP_UPDATE) {
            if (resultCode == Activity.RESULT_OK) {
                onUpdateAgreed(requestData)
            } else {
                onUpdateDenied(requestData)
            }
        }
        pendingRequestData = null
    }

    private fun onUpdateAgreed(data: InAppUpdateManager.InAppUpdateData) {
        TelemetryWrapper.clickGooglePlayDialog(data, true)
        inAppUpdateManager.onUpdateAgreed()
    }

    private fun onUpdateDenied(data: InAppUpdateManager.InAppUpdateData) {
        TelemetryWrapper.clickGooglePlayDialog(data, false)
        inAppUpdateManager.onUpdateDenied()
    }

    private fun showIntroDialog(data: InAppUpdateManager.InAppUpdateData, delegate: ViewDelegate) {
        TelemetryWrapper.showIntroDialog(data)
        val intro = data.config.introData ?: return
        delegate.showIntroDialog(intro, {
            TelemetryWrapper.clickIntroDialog(data, true)
            inAppUpdateManager.onIntroAgreed(data)
        }, {
            TelemetryWrapper.clickIntroDialog(data, false)
            inAppUpdateManager.onIntroDenied(data)
        })
    }

    private fun showInstallPrompt(
        data: InAppUpdateManager.InAppUpdateData,
        delegate: ViewDelegate
    ) {
        TelemetryWrapper.showInstallPrompt(data.info.availableVersionCode(), isExistingDownload = true)

        delegate.showInstallPrompt {
            inAppUpdateManager.onInstallAgreed()
        }
        delegate.showInstallPromptNotification()
    }

    private fun showInstallPromptForExistDownload(
        data: InAppUpdateManager.InAppUpdateData,
        delegate: ViewDelegate
    ) {
        TelemetryWrapper.showInstallPrompt(data.info.availableVersionCode(), isExistingDownload = false)

        delegate.showInstallPrompt {
            inAppUpdateManager.onInstallExistingDownloadAgreed()
        }
    }

    private fun showDownloadStartHint(delegate: ViewDelegate) {
        delegate.showDownloadStartHint()
    }

    private fun startUpdate(
        activity: Activity,
        manager: AppUpdateManager,
        data: InAppUpdateManager.InAppUpdateData
    ) {
        pendingRequestData = data
        manager.registerListener(createUpdateListener(manager, data))
        manager.startUpdateFlowForResult(
                data.info,
                AppUpdateType.FLEXIBLE,
                activity,
                MainActivity.REQUEST_CODE_IN_APP_UPDATE
        )
        TelemetryWrapper.showGooglePlayDialog(data)
    }

    private fun startInstall(
        manager: AppUpdateManager,
        isExistingDownload: Boolean,
        isFromNotification: Boolean
    ) {
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() != InstallStatus.DOWNLOADED) {
                log("no downloaded update, skip install")
                return@addOnSuccessListener
            }

            TelemetryWrapper.clickInstallPrompt(
                    info.availableVersionCode(),
                    isExistingDownload,
                    isFromNotification
            )

            manager.completeUpdate().addOnSuccessListener {
                inAppUpdateManager.onInstallSuccess()
            }.addOnFailureListener {
                inAppUpdateManager.onInstallFailed()
            }.addOnCompleteListener {
                inAppUpdateManager.onInstallComplete()
            }
        }
    }

    private fun createUpdateListener(
        manager: AppUpdateManager,
        data: InAppUpdateManager.InAppUpdateData
    ): InstallStateUpdatedListener {
        return object : InstallStateUpdatedListener {
            override fun onStateUpdate(state: InstallState) {
                when (state.installStatus()) {
                    InstallStatus.DOWNLOADED -> {
                        log("download complete")
                        manager.unregisterListener(this)
                        inAppUpdateManager.onUpdateDownloaded(data)
                    }
                    InstallStatus.FAILED -> {
                        log("download failed")
                        manager.unregisterListener(this)
                        inAppUpdateManager.onUpdateDownloadFailed()
                    }
                    InstallStatus.CANCELED -> {
                        log("download cancelled")
                        manager.unregisterListener(this)
                        inAppUpdateManager.onUpdateDownloadCancelled()
                    }
                    else -> {
                        log("unhandled install status: ${state.installStatus()}")
                    }
                }
            }
        }
    }

    private fun isInAppUpdateInstallIntent(intent: Intent?): Boolean {
        intent ?: return false

        val action = intent.action
        return action != null && action == ACTION_INSTALL_IN_APP_UPDATE
    }

    private fun log(msg: String) {
        if (inAppUpdateModel.isInAppUpdateLogEnabled()) {
            Log.d(TAG, msg)
        }
    }

    interface ViewDelegate {
        fun showIntroDialog(
            data: InAppUpdateIntro,
            positiveCallback: () -> Unit,
            negativeCallback: () -> Unit
        ): Boolean
        fun showInstallPrompt(actionCallback: () -> Unit)
        fun showInstallPromptNotification()
        fun showDownloadStartHint()
    }

    companion object {
        private const val TAG = "InAppUpdateController"
    }
}

private fun TelemetryWrapper.showIntroDialog(data: InAppUpdateManager.InAppUpdateData) {
    showInAppUpdateIntroDialog(data.info.availableVersionCode())
}

private fun TelemetryWrapper.clickIntroDialog(
    data: InAppUpdateManager.InAppUpdateData,
    isAccepted: Boolean
) {
    clickInAppUpdateIntroDialog(
            if (isAccepted) {
                TelemetryWrapper.Value.POSITIVE
            } else {
                TelemetryWrapper.Value.NEGATIVE
            },
            data.config.forceCloseOnDenied,
            data.info.availableVersionCode()
    )
}

private fun TelemetryWrapper.showGooglePlayDialog(data: InAppUpdateManager.InAppUpdateData) {
    showInAppUpdateGooglePlayDialog(data.info.availableVersionCode())
}

private fun TelemetryWrapper.clickGooglePlayDialog(
    data: InAppUpdateManager.InAppUpdateData,
    isAccepted: Boolean
) {
    clickInAppUpdateGooglePlayDialog(
            if (isAccepted) {
                TelemetryWrapper.Value.POSITIVE
            } else {
                TelemetryWrapper.Value.NEGATIVE
            },
            data.config.forceCloseOnDenied,
            data.info.availableVersionCode()
    )
}

private fun TelemetryWrapper.showInstallPrompt(toVersion: Int, isExistingDownload: Boolean) {
    showInAppUpdateInstallPrompt(
            if (isExistingDownload) {
                TelemetryWrapper.Extra_Value.REMINDER
            } else {
                TelemetryWrapper.Extra_Value.NEW
            },
            toVersion
    )
}

private fun TelemetryWrapper.clickInstallPrompt(
    toVersion: Int,
    isExistingDownload: Boolean,
    isFromNotification: Boolean
) {
    clickInAppUpdateInstallPrompt(
            if (isFromNotification) {
                TelemetryWrapper.Extra_Value.NOTIFICATION
            } else {
                TelemetryWrapper.Extra_Value.SNACKBAR
            },
            if (isExistingDownload) {
                TelemetryWrapper.Extra_Value.REMINDER
            } else {
                TelemetryWrapper.Extra_Value.NEW
            },
            toVersion
    )
}
