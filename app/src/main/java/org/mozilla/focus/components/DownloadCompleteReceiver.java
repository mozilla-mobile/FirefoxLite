/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.components;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;

import org.mozilla.focus.download.DownloadInfo;
import org.mozilla.focus.download.DownloadInfoManager;
import org.mozilla.focus.utils.ThreadUtils;

import java.io.File;
import java.util.List;

public class DownloadCompleteReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);

        if (downloadId == -1) {
            return;
        }

        DownloadInfoManager.getInstance().queryByDownloadId(downloadId, new DownloadInfoManager.AsyncQueryListener() {
            @Override
            public void onQueryComplete(List downloadInfoList) {
                if (downloadInfoList.size() > 0) {
                    final DownloadInfo downloadInfo = (DownloadInfo) downloadInfoList.get(0);
                    if ((downloadInfo.getStatus() == DownloadManager.STATUS_SUCCESSFUL)
                            && !TextUtils.isEmpty(downloadInfo.getFileUri())) {

                        // have to update, then the fileUri may write into our DB.
                        DownloadInfoManager.getInstance().updateByRowId(downloadInfo, new DownloadInfoManager.AsyncUpdateListener() {
                            @Override
                            public void onUpdateComplete(int result) {
                                final Uri fileUri = Uri.parse(downloadInfo.getFileUri());
                                if ("file".equals(fileUri.getScheme())) {
                                    ThreadUtils.postToBackgroundThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            // on some device the uri is "file:///storage/emulated/0/Download/file.png"
                                            // but the real path is "file:///storage/emulated/legacy/Download/file.png"
                                            // Since we already restrict download folder when we were making request to
                                            // DownloadManager, now we only look for the file-name in download folder.
                                            final String fileName = (new File(fileUri.getPath())).getName();
                                            final String type = Environment.DIRECTORY_DOWNLOADS;
                                            final File dir = Environment.getExternalStoragePublicDirectory(type);
                                            final File downloadedFile = new File(dir, fileName);

                                            if (downloadedFile.exists() && downloadedFile.canWrite()) {
                                                RelocateService.startActionMove(context,
                                                        downloadInfo.getRowId(),
                                                        downloadInfo.getDownloadId(),
                                                        downloadedFile,
                                                        downloadInfo.getMimeType());
                                            }
                                        }
                                    });
                                }
                            }
                        });
                    }
                    // Download canceled
                    if (!downloadInfo.existInDownloadManager()) {
                        DownloadInfoManager.getInstance().delete(downloadInfo.getRowId(), null);
                    }
                }
            }
        });
    }
}
