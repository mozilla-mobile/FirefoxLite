/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.rocket.download.data

import android.Manifest
import android.app.IntentService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.mozilla.fileutils.FileUtils
import org.mozilla.focus.R
import org.mozilla.focus.notification.NotificationId
import org.mozilla.focus.utils.Constants
import org.mozilla.focus.utils.NoRemovableStorageException
import org.mozilla.focus.utils.Settings
import org.mozilla.focus.utils.StorageUtils
import org.mozilla.rocket.content.appComponent
import java.io.File
import javax.inject.Inject

/**
 * A service to help on moving downloaded file to another storage directory
 */
class RelocateService : IntentService(TAG) {

    @Inject
    lateinit var downloadInfoRepository: DownloadInfoRepository

    override fun onCreate() {
        appComponent().inject(this)
        super.onCreate()
    }

    private fun startForeground() {
        val notificationChannelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            configForegroundChannel(this)
            CHANNEL_ID
        } else {
            "not_used_notification_id"
        }
        val builder = NotificationCompat.Builder(applicationContext, notificationChannelId)
        val notification = builder.build()
        startForeground(NotificationId.RELOCATE_SERVICE, notification)
    }

    private fun stopForeground() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    override fun onHandleIntent(intent: Intent?) {
        if (intent != null) {
            val action = intent.action
            if (ACTION_MOVE == action) {
                // if the download id is not in our database, ignore this operation
                val downloadId = intent.getLongExtra(Constants.EXTRA_DOWNLOAD_ID, -1)
                if (!downloadInfoRepository.hasDownloadItem(downloadId)) {
                    return
                }

                // return if no file to move
                val src = File(intent.getStringExtra(Constants.EXTRA_FILE_PATH))
                if (!src.exists() || !src.canWrite()) {
                    return
                }
                val rowId = intent.getLongExtra(Constants.EXTRA_ROW_ID, -1)
                val type = intent.type
                startForeground()
                GlobalScope.launch {
                    handleActionMove(rowId, downloadId, src, type)
                    stopForeground()
                }
            }
        }
    }

    /**
     * Handle action Move in the provided background thread with the provided
     * parameters.
     */
    private suspend fun handleActionMove(
        rowId: Long,
        downloadId: Long,
        srcFile: File,
        mediaType: String?
    ) {
        val settings = Settings.getInstance(applicationContext)
        // Do nothing, if user turned off the option
        if (!settings.shouldSaveToRemovableStorage()) {
            broadcastRelocateFinished(rowId)
            return
        }

        // To move file if we have correct permission
        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            moveFile(rowId, downloadId, srcFile, mediaType)
        } else {
            // if no permission, send broadcast to UI.
            // If get permission from UI, it should startService again
            broadcastNoPermission(downloadId, srcFile, mediaType)
        }
    }

    /**
     * If removable storage exists, to move a file to it. Once moving completed, also update
     * database for latest file path.
     *
     * @param rowId id of downloaded file in our database
     * @param downloadId downloadId of downloaded file, need this to update system database
     * @param srcFile file to be moved
     * @param type MIME type of the file, to decide sub directory
     */
    private suspend fun moveFile(rowId: Long, downloadId: Long, srcFile: File, type: String?) {
        val settings = Settings.getInstance(applicationContext)
        var destFile: File? = null
        try {
            val outputDir = StorageUtils.getTargetDirOnRemovableStorageForDownloads(this, type)
            if (outputDir != null) {
                FileUtils.ensureDir(outputDir)
                destFile = FileUtils.getFileSlot(outputDir, srcFile.name)
                if (outputDir.usableSpace < srcFile.length()) {
                    val msg: CharSequence = getString(R.string.message_removable_storage_space_not_enough)
                    broadcastUi(msg)
                    Log.w(TAG, msg.toString())
                    broadcastRelocateFinished(rowId)
                    return
                }

                // instead of rename, to use copy + remove for safety
                val copied = FileUtils.copy(srcFile, destFile)
                if (!copied) {
                    Log.w(TAG, String.format("cannot copy file from %s to %s",
                        srcFile.path, destFile.path))
                    broadcastRelocateFinished(rowId)
                    return
                }
                val deleted = srcFile.delete()
                if (!deleted) {
                    throw RuntimeException("Cannot delete original file: " + srcFile.absolutePath)
                }

                // downloaded file is moved, update database to reflect this changing
                downloadInfoRepository.replaceFilePath(downloadId, destFile.absolutePath, type)

                // removable-storage did not exist on app creation, but now it is back
                // we moved download file to removable-storage, now we should inform user
                if (!settings.removableStorageStateOnCreate) {
                    // avoid sending same message continuously
                    if (settings.showedStorageMessage != Settings.STORAGE_MSG_TYPE_REMOVABLE_AVAILABLE) {
                        settings.showedStorageMessage = Settings.STORAGE_MSG_TYPE_REMOVABLE_AVAILABLE
                        val msg: CharSequence = getString(R.string.message_start_to_save_to_removable_storage)
                        broadcastUi(msg)
                        Log.w(TAG, msg.toString())
                    }
                }
            }
        } catch (e: NoRemovableStorageException) {
            // removable-storage existed on app creation, but now it is gone
            // we keep download file in original path, now we should inform user
            broadcastRelocateFinished(rowId)
            if (settings.removableStorageStateOnCreate) {
                // avoid sending same message continuously
                if (settings.showedStorageMessage != Settings.STORAGE_MSG_TYPE_REMOVABLE_UNAVAILABLE) {
                    settings.showedStorageMessage = Settings.STORAGE_MSG_TYPE_REMOVABLE_UNAVAILABLE
                    val msg: CharSequence = getString(R.string.message_fallback_save_to_primary_external)
                    broadcastUi(msg)
                    Log.w(TAG, msg.toString())
                }
            }
            e.printStackTrace()
        } catch (e: Exception) {
            // if anything wrong, try to keep original file
            broadcastRelocateFinished(rowId)
            try {
                if (destFile != null && destFile.exists() && destFile.canWrite() && srcFile.exists()) {
                    if (destFile.delete()) {
                        Log.w(TAG, "cannot delete copied file: " + destFile.absolutePath)
                    }
                }
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
            e.printStackTrace()
        }
    }

    /**
     * Send a broadcast to foreground component to notify user any messages.
     *
     * @param msg
     */
    private fun broadcastUi(msg: CharSequence) {
        val broadcastIntent = Intent(Constants.ACTION_NOTIFY_UI)
        broadcastIntent.addCategory(Constants.CATEGORY_FILE_OPERATION)
        broadcastIntent.putExtra(Constants.EXTRA_MESSAGE, msg)
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)
    }

    private fun broadcastNoPermission(
        downloadId: Long,
        srcFile: File,
        mediaType: String?
    ) {
        val broadcastIntent = Intent(Constants.ACTION_REQUEST_PERMISSION)
        broadcastIntent.addCategory(Constants.CATEGORY_FILE_OPERATION)

        // append extra information so UI can restart this service
        broadcastIntent.putExtra(Constants.EXTRA_DOWNLOAD_ID, downloadId)
        broadcastIntent.putExtra(Constants.EXTRA_FILE_PATH, srcFile.absoluteFile)
        broadcastIntent.type = mediaType
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)
        Log.d(TAG, "no permission for file relocating, send broadcast to grant permission")
    }

    private fun broadcastRelocateFinished(rowId: Long) {
        broadcastRelocateFinished(this, rowId)
    }

    companion object {
        private const val TAG = "RelocateService"
        private const val ACTION_MOVE = "org.mozilla.focus.components.action.MOVE"
        private const val CHANNEL_ID = "relocation_service"

        /**
         * Starts this service to perform action Move with the given parameters. If
         * the service is already performing a task this action will be queued.
         *
         * @see IntentService
         */
        fun startActionMove(
            context: Context,
            rowId: Long,
            downloadId: Long,
            srcFile: File,
            mediaType: String?
        ) {
            val intent = Intent(context, RelocateService::class.java)
            intent.action = ACTION_MOVE
            intent.putExtra(Constants.EXTRA_ROW_ID, rowId)
            intent.putExtra(Constants.EXTRA_DOWNLOAD_ID, downloadId)
            intent.putExtra(Constants.EXTRA_FILE_PATH, srcFile.absolutePath)
            intent.type = mediaType
            ContextCompat.startForegroundService(context, intent)
        }

        // Configure the notification channel if needed
        private fun configForegroundChannel(context: Context) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // NotificationChannel API is only available for Android O and above, so we need to add the check here so IDE won't complain
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channelName = context.getString(R.string.app_name)
                val notificationChannel = NotificationChannel(CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH)
                notificationManager.createNotificationChannel(notificationChannel)
            }
        }

        fun broadcastRelocateFinished(context: Context, rowId: Long) {
            val broadcastIntent = Intent(Constants.ACTION_NOTIFY_RELOCATE_FINISH)
            broadcastIntent.addCategory(Constants.CATEGORY_FILE_OPERATION)
            broadcastIntent.putExtra(Constants.EXTRA_ROW_ID, rowId)
            LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent)
        }
    }
}