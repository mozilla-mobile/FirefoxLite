/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.components;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.mozilla.focus.R;
import org.mozilla.focus.download.DownloadInfoManager;
import org.mozilla.focus.utils.Constants;
import org.mozilla.focus.utils.FileUtils;
import org.mozilla.focus.utils.NoRemovableStorageException;
import org.mozilla.focus.utils.Settings;
import org.mozilla.focus.utils.StorageUtils;
import org.mozilla.rocket.util.ForeGroundIntentService;

import java.io.File;

/**
 * A service to help on moving downloaded file to another storage directory
 */
public class RelocateService extends ForeGroundIntentService {

    private static final String TAG = "RelocateService";
    private static final String ACTION_MOVE = "org.mozilla.focus.components.action.MOVE";
    private static final String CHANNEL_ID = "relocation_service";

    public RelocateService() {
        super(TAG);
    }

    /**
     * Starts this service to perform action Move with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see android.app.IntentService
     */
    public static void startActionMove(@NonNull Context context,
                                       long rowId,
                                       long downloadId,
                                       @NonNull File srcFile,
                                       @Nullable String mediaType) {

        final Intent intent = new Intent(context, RelocateService.class);
        intent.setAction(ACTION_MOVE);
        intent.putExtra(Constants.EXTRA_ROW_ID, rowId);
        intent.putExtra(Constants.EXTRA_DOWNLOAD_ID, downloadId);
        intent.putExtra(Constants.EXTRA_FILE_PATH, srcFile.getAbsolutePath());
        intent.setType(mediaType);
        ContextCompat.startForegroundService(context, intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_MOVE.equals(action)) {

                // if the download id is not in our database, ignore this operation
                final long downloadId = intent.getLongExtra(Constants.EXTRA_DOWNLOAD_ID, -1);
                final DownloadInfoManager mgr = DownloadInfoManager.getInstance();
                if (!mgr.recordExists(downloadId)) {
                    return;
                }

                // return if no file to move
                final File src = new File(intent.getStringExtra(Constants.EXTRA_FILE_PATH));
                if (!src.exists() || !src.canWrite()) {
                    return;
                }

                final long rowId = intent.getLongExtra(Constants.EXTRA_ROW_ID, -1);
                final String type = intent.getType();
                startForeground();
                handleActionMove(rowId, downloadId, src, type);
                stopForeground();
            }
        }
    }

    /**
     * Handle action Move in the provided background thread with the provided
     * parameters.
     */
    private void handleActionMove(final long rowId,
                                  final long downloadId,
                                  @NonNull final File srcFile,
                                  @Nullable final String mediaType) {

        final Settings settings = Settings.getInstance(getApplicationContext());
        // Do nothing, if user turned off the option
        if (!settings.shouldSaveToRemovableStorage()) {
            broadcastRelocateFinished(rowId);
            return;
        }

        // To move file if we have correct permission
        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            moveFile(rowId, downloadId, srcFile, mediaType);
        } else {
            // if no permission, send broadcast to UI.
            // If get permission from UI, it should startService again
            broadcastNoPermission(downloadId, srcFile, mediaType);
        }
    }

    /**
     * If removable storage exists, to move a file to it. Once moving completed, also update
     * database for latest file path.
     *
     * @param rowId      id of downloaded file in our database
     * @param downloadId downloadId of downloaded file, need this to update system database
     * @param srcFile    file to be moved
     * @param type       MIME type of the file, to decide sub directory
     */
    private void moveFile(final long rowId, final long downloadId, final File srcFile, final String type) {
        final Settings settings = Settings.getInstance(getApplicationContext());
        File destFile = null;
        try {
            final File outputDir = StorageUtils.getTargetDirOnRemovableStorageForDownloads(this, type);
            if (outputDir != null) {
                FileUtils.ensureDir(outputDir);
                destFile = FileUtils.getFileSlot(outputDir, srcFile.getName());

                if (outputDir.getUsableSpace() < srcFile.length()) {
                    final CharSequence msg = getString(R.string.message_removable_storage_space_not_enough);
                    broadcastUi(msg);
                    Log.w(TAG, msg.toString());
                    broadcastRelocateFinished(rowId);
                    return;
                }

                // instead of rename, to use copy + remove for safety
                boolean copied = FileUtils.copy(srcFile, destFile);
                if (!copied) {
                    Log.w(TAG, String.format("cannot copy file from %s to %s",
                            srcFile.getPath(), destFile.getPath()));
                    broadcastRelocateFinished(rowId);
                    return;
                }

                boolean deleted = srcFile.delete();
                if (!deleted) {
                    throw new RuntimeException("Cannot delete original file: " + srcFile.getAbsolutePath());
                }

                // downloaded file is moved, update database to reflect this changing
                final DownloadInfoManager mgr = DownloadInfoManager.getInstance();
                mgr.replacePath(downloadId, destFile.getAbsolutePath(), type);

                // removable-storage did not exist on app creation, but now it is back
                // we moved download file to removable-storage, now we should inform user
                if (!settings.getRemovableStorageStateOnCreate()) {

                    // avoid sending same message continuously
                    if (settings.getShowedStorageMessage() != Settings.STORAGE_MSG_TYPE_REMOVABLE_AVAILABLE) {
                        settings.setShowedStorageMessage(Settings.STORAGE_MSG_TYPE_REMOVABLE_AVAILABLE);
                        final CharSequence msg = getString(R.string.message_start_to_save_to_removable_storage);
                        broadcastUi(msg);
                        Log.w(TAG, msg.toString());
                    }
                }
            }
        } catch (NoRemovableStorageException e) {
            // removable-storage existed on app creation, but now it is gone
            // we keep download file in original path, now we should inform user
            broadcastRelocateFinished(rowId);
            if (settings.getRemovableStorageStateOnCreate()) {

                // avoid sending same message continuously
                if (settings.getShowedStorageMessage() != Settings.STORAGE_MSG_TYPE_REMOVABLE_UNAVAILABLE) {
                    settings.setShowedStorageMessage(Settings.STORAGE_MSG_TYPE_REMOVABLE_UNAVAILABLE);
                    final CharSequence msg = getString(R.string.message_fallback_save_to_primary_external);
                    broadcastUi(msg);
                    Log.w(TAG, msg.toString());
                }
            }

            e.printStackTrace();
        } catch (Exception e) {
            // if anything wrong, try to keep original file
            broadcastRelocateFinished(rowId);
            try {
                if ((destFile != null)
                        && destFile.exists()
                        && destFile.canWrite()
                        && srcFile.exists()) {
                    if (destFile.delete()) {
                        Log.w(TAG, "cannot delete copied file: " + destFile.getAbsolutePath());
                    }
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
            e.printStackTrace();
        }
    }

    /**
     * Send a broadcast to foreground component to notify user any messages.
     *
     * @param msg
     */
    private void broadcastUi(@NonNull CharSequence msg) {
        final Intent broadcastIntent = new Intent(Constants.ACTION_NOTIFY_UI);
        broadcastIntent.addCategory(Constants.CATEGORY_FILE_OPERATION);
        broadcastIntent.putExtra(Constants.EXTRA_MESSAGE, msg);

        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
    }

    private void broadcastNoPermission(final long downloadId,
                                       @NonNull final File srcFile,
                                       @Nullable final String mediaType) {

        final Intent broadcastIntent = new Intent(Constants.ACTION_REQUEST_PERMISSION);
        broadcastIntent.addCategory(Constants.CATEGORY_FILE_OPERATION);

        // append extra information so UI can restart this service
        broadcastIntent.putExtra(Constants.EXTRA_DOWNLOAD_ID, downloadId);
        broadcastIntent.putExtra(Constants.EXTRA_FILE_PATH, srcFile.getAbsoluteFile());
        broadcastIntent.setType(mediaType);

        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
        Log.d(TAG, "no permission for file relocating, send broadcast to grant permission");
    }

    private void broadcastRelocateFinished(long rowId) {
        broadcastRelocateFinished(this, rowId);
    }

    public static void broadcastRelocateFinished(Context context, long rowId) {
        final Intent broadcastIntent = new Intent(Constants.ACTION_NOTIFY_RELOCATE_FINISH);
        broadcastIntent.addCategory(Constants.CATEGORY_FILE_OPERATION);
        broadcastIntent.putExtra(Constants.EXTRA_ROW_ID, rowId);

        LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);

    }

    @Override
    protected String getNotificationId() {
        return CHANNEL_ID;
    }
}
