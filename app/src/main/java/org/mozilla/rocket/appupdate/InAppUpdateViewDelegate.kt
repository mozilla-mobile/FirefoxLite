/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.appupdate

import android.app.PendingIntent
import android.content.Intent
import android.support.design.widget.Snackbar
import android.support.v4.app.NotificationCompat
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.Toast
import org.mozilla.focus.R
import org.mozilla.focus.activity.MainActivity
import org.mozilla.focus.activity.MainActivity.ACTION_INSTALL_IN_APP_UPDATE
import org.mozilla.focus.notification.NotificationId
import org.mozilla.focus.notification.NotificationUtil
import org.mozilla.focus.utils.AppConstants
import org.mozilla.rocket.landing.DialogQueue

class InAppUpdateViewDelegate(
    private val activity: MainActivity,
    private val dialogQueue: DialogQueue,
    private val snackBarAnchor: View
) : InAppUpdateManager.InAppUpdateUIDelegate {

    override fun showInAppUpdateIntro(
        callback: InAppUpdateManager.InAppUpdateIntroCallback,
        data: InAppUpdateIntro
    ): Boolean {
        val dialog = AlertDialog.Builder(activity)
                .setTitle(data.title)
                .setMessage(data.description)
                .setPositiveButton(data.positiveText) { _, _ -> callback.onPositive() }
                .setNegativeButton(data.negativeText) { _, _ -> callback.onNegative() }
                .setCancelable(false)
                .create()
        dialog.setCanceledOnTouchOutside(false)
        dialogQueue.tryShow(object : DialogQueue.DialogDelegate {
            override fun setOnDismissListener(listener: () -> Unit) {
                dialog.setOnDismissListener {
                    listener()
                }
            }

            override fun show() {
                dialog.show()
            }
        })
        return true
    }

    override fun showInAppUpdateInstallPrompt(action: () -> Unit) {
        postInstallPromptNotification()

        Snackbar.make(
                snackBarAnchor,
                activity.getString(R.string.update_to_latest_app_snack_bar_message),
                Snackbar.LENGTH_LONG
        ).setAction(activity.getString(R.string.update_to_latest_app_snack_bar_update)) {
            action.invoke()
        }.show()
    }

    override fun showInAppUpdateDownloadStartHint() {
        postDownloadingNotification()

        Toast.makeText(
                activity,
                activity.getString(R.string.update_to_latest_app_toast),
                Toast.LENGTH_SHORT
        ).show()
    }

    override fun closeOnInAppUpdateDenied() {
        activity.finish()
    }

    fun postInstallPromptNotification() {
        val intent = Intent(ACTION_INSTALL_IN_APP_UPDATE).apply {
            setClassName(activity, AppConstants.LAUNCHER_ACTIVITY_ALIAS)
        }

        val pendingIntent = PendingIntent.getActivity(
                activity,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationUtil.baseBuilder(activity, NotificationUtil.Channel.LOW_PRIORITY)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentTitle(activity.getString(R.string.update_to_latest_app_notification))
                .setLargeIcon(null)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

        NotificationUtil.sendNotification(activity, NotificationId.IN_APP_UPDATE, builder)
    }

    private fun postDownloadingNotification() {
        val builder = NotificationUtil.baseBuilder(activity, NotificationUtil.Channel.LOW_PRIORITY)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentTitle(activity.getString(R.string.update_to_latest_app_toast))
                .setAutoCancel(true)

        NotificationUtil.sendNotification(activity, NotificationId.IN_APP_UPDATE, builder)
    }
}
