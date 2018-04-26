package org.mozilla.focus.download;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.widget.Toast;

import org.mozilla.focus.R;
import org.mozilla.focus.components.RelocateService;
import org.mozilla.focus.utils.Constants;
import org.mozilla.focus.web.Download;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Use Android's Download Manager to queue this download.
 */
public class EnqueueDownloadTask extends AsyncTask<Void, Void, EnqueueDownloadTask.ErrorCode> {

    private WeakReference<Activity> activityRef;
    private Download download;
    private String refererUrl;

    public EnqueueDownloadTask(@NonNull Activity activity, @NonNull Download download, @Nullable String refererUrl) {
        this.activityRef = new WeakReference<>(activity);
        this.download = download;
        this.refererUrl = refererUrl;
    }

    enum ErrorCode {
        SUCCESS, GENERAL_ERROR, STORAGE_UNAVAILABLE, FILE_NOT_SUPPORTED
    }

    @Override
    protected ErrorCode doInBackground(Void... params) {
        final Context context = activityRef.get();
        if (context == null) {
            return ErrorCode.GENERAL_ERROR;
        }

        final String cookie = CookieManager.getInstance().getCookie(download.getUrl());
        final String fileName = URLUtil.guessFileName(
                download.getUrl(), download.getContentDisposition(), download.getMimeType());

        // so far each download always return null even for an image.
        // But we might move downloaded file to another directory.
        // So, for now we always save file to DIRECTORY_DOWNLOADS
        final String dir = Environment.DIRECTORY_DOWNLOADS;

        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return ErrorCode.STORAGE_UNAVAILABLE;
        }

        // block non-http/https download links
        if (!URLUtil.isNetworkUrl(download.getUrl())) {
            return ErrorCode.FILE_NOT_SUPPORTED;
        }

        final DownloadManager.Request request = new DownloadManager.Request(Uri.parse(download.getUrl()))
                .addRequestHeader("User-Agent", download.getUserAgent())
                .addRequestHeader("Cookie", cookie)
                .addRequestHeader("Referer", refererUrl)
                .setDestinationInExternalPublicDir(dir, fileName)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setMimeType(download.getMimeType());

        request.allowScanningByMediaScanner();

        final DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        final Long downloadId = manager.enqueue(request);

        DownloadInfo downloadInfo = new DownloadInfo();
        downloadInfo.setDownloadId(downloadId);
        // On Pixel, When downloading downloaded content which is still available, DownloadManager
        // returns the previous download id.
        // For that case we remove the old entry and re-insert a new one to move it to the top.
        // (Note that this is not the case for devices like Samsung, I have not verified yet if this
        // is a because of on those devices we move files to SDcard or if this is true even if the
        // file is not moved.)
        if (!DownloadInfoManager.getInstance().recordExists(downloadId)) {
            DownloadInfoManager.getInstance().insert(downloadInfo, new DownloadInfoManager.AsyncInsertListener() {
                @Override
                public void onInsertComplete(long id) {
                    DownloadInfoManager.notifyRowUpdated(context, id);
                }
            });
        } else {
            DownloadInfoManager.getInstance().queryByDownloadId(downloadId, new DownloadInfoManager.AsyncQueryListener() {
                @Override
                public void onQueryComplete(List downloadInfoList) {
                    if (!downloadInfoList.isEmpty()) {
                        DownloadInfo info = (DownloadInfo) downloadInfoList.get(0);
                        DownloadInfoManager.getInstance().delete(info.getRowId(), null);
                        DownloadInfoManager.getInstance().insert(info, new DownloadInfoManager.AsyncInsertListener() {
                            @Override
                            public void onInsertComplete(long rowId) {
                                DownloadInfoManager.notifyRowUpdated(context, rowId);
                                RelocateService.broadcastRelocateFinished(context, rowId);
                            }
                        });
                    }
                }
            });
        }

        return ErrorCode.SUCCESS;
    }

    @Override
    protected void onPostExecute(ErrorCode errorCode) {
        final Activity activity = activityRef.get();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        switch (errorCode) {
            case STORAGE_UNAVAILABLE:
                Toast.makeText(activity,
                        R.string.message_storage_unavailable_cancel_download,
                        Toast.LENGTH_LONG)
                        .show();
                break;
            case FILE_NOT_SUPPORTED:
                Toast.makeText(activity, R.string.download_file_not_supported, Toast.LENGTH_LONG).show();
                break;
            case SUCCESS:
                if (!download.isStartFromContextMenu()) {
                    Toast.makeText(activity, R.string.download_started, Toast.LENGTH_LONG).show();
                }
                break;
            case GENERAL_ERROR:
            default:
                break;
        }
    }
}
