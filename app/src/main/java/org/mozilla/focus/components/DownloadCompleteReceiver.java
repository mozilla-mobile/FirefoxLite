/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.components;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;

import org.mozilla.focus.download.DownloadInfo;
import org.mozilla.focus.download.DownloadInfoManager;

import java.io.File;

public class DownloadCompleteReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);

        if (downloadId == -1) {
            return;
        }

        final DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        final DownloadManager.Query query = new DownloadManager.Query();

        query.setFilterById(downloadId);
        final Cursor cursor = downloadManager.query(query);

        if (cursor.moveToFirst()) {
            final int downloadStatus = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            final String localUriStr = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
            final String mediaType = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE));
            if ((downloadStatus == DownloadManager.STATUS_SUCCESSFUL)
                    && !TextUtils.isEmpty(localUriStr)) {

                //update download info in our db
                DownloadInfo downloadInfo = new DownloadInfo();
                downloadInfo.setDownloadId(downloadId);
                downloadInfo.setFileName(new File(localUriStr).getName());
                downloadInfo.setFileUri(localUriStr);
                DownloadInfoManager.getInstance().update(downloadInfo,null);

                final Uri fileUri = Uri.parse(localUriStr);
                if ("file".equals(fileUri.getScheme())) {

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
                                downloadId,
                                downloadedFile,
                                mediaType);
                    }
                }
            }
        }
        cursor.close();
    }
}
