/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget;

import android.app.DownloadManager;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.mozilla.focus.R;
import org.mozilla.focus.download.DownloadInfo;
import org.mozilla.focus.download.DownloadInfoManager;
import org.mozilla.focus.fragment.PanelFragment;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.utils.IntentUtils;
import org.mozilla.rocket.download.DownloadInfoViewModel;
import org.mozilla.threadutils.ThreadUtils;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Created by anlin on 01/08/2017.
 */

public class DownloadListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int ACTION_DEFAULT = 0;
    private static final int ACTION_CANCEL = 1;
    private static final List<String> SPECIFIC_FILE_EXTENSION
            = Arrays.asList("apk", "zip", "gz", "tar", "7z", "rar", "war");
    private List<DownloadInfo> mDownloadInfo;
    private Context mContext;
    private DownloadInfoViewModel viewModel;

    public DownloadListAdapter(Context context, @NonNull DownloadInfoViewModel model) {
        mContext = context;
        viewModel = model;
        viewModel.loadMore(true);
        viewModel.setOpening(true);
    }

    public void setList(List<DownloadInfo> list) {
        if (mDownloadInfo == null) {
            mDownloadInfo = list;
        }
    }

    private void delete(final View view, final long rowId) {
        viewModel.delete(rowId, downloadInfo -> {
            final File file = new File(URI.create(downloadInfo.getFileUri()).getPath());
            if (file.exists()) {
                final Snackbar snackBar = getDeleteSnackBar(view, downloadInfo);
                snackBar.show();
            } else {
                Toast.makeText(mContext, R.string.cannot_find_the_file, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cancel(final long rowId) {
        viewModel.cancelDownload(rowId, downloadInfo -> {
            final String cancelStr = mContext.getString(R.string.download_cancel);
            Toast.makeText(mContext, cancelStr, Toast.LENGTH_SHORT).show();
        });
    }


    @Override
    public int getItemViewType(int position) {
        if (viewModel.isOpening()) {
            return PanelFragment.ON_OPENING;
        } else {
            if (mDownloadInfo == null || mDownloadInfo.isEmpty()) {
                return PanelFragment.VIEW_TYPE_EMPTY;
            } else {
                return PanelFragment.VIEW_TYPE_NON_EMPTY;
            }
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView;
        if (PanelFragment.VIEW_TYPE_NON_EMPTY == viewType) {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.download_menu_cell, parent, false);
            return new DownloadViewHolder(itemView);
        } else if (PanelFragment.ON_OPENING == viewType) {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.download_empty, parent, false);
            return new OnOpeningViewHolder(itemView);
        } else {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.download_empty, parent, false);
            return new DownloadEmptyViewHolder(itemView);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {

        if (viewHolder instanceof DownloadViewHolder) {
            DownloadViewHolder holder = (DownloadViewHolder) viewHolder;
            DownloadInfo downloadInfo = mDownloadInfo.get(position);

            if (!TextUtils.isEmpty(downloadInfo.getFileName())) {
                holder.title.setText(downloadInfo.getFileName());
            } else {
                holder.title.setText(R.string.unknown);
            }

            holder.icon.setImageResource(mappingIcon(downloadInfo));

            String subtitle = "";
            if (DownloadManager.STATUS_SUCCESSFUL == downloadInfo.getStatus()) {
                subtitle = downloadInfo.getSize() + ", " + downloadInfo.getDate();
                holder.progressBar.setVisibility(View.GONE);
                holder.action.setImageLevel(ACTION_DEFAULT);
            } else if (DownloadManager.STATUS_RUNNING == downloadInfo.getStatus()) {
                final int progress = (int) (100 * downloadInfo.getSizeSoFar() / downloadInfo.getSizeTotal());
                holder.progressBar.setProgress(progress);
                holder.progressBar.setVisibility(View.VISIBLE);
                subtitle = progress + "%";
                holder.action.setImageLevel(ACTION_CANCEL);
            } else {
                subtitle = statusConvertStr(downloadInfo.getStatus());
                holder.progressBar.setVisibility(View.GONE);
                holder.action.setImageLevel(ACTION_DEFAULT);
            }

            holder.subtitle.setText(subtitle);
            holder.action.setTag(R.id.status, downloadInfo.getStatus());
            holder.action.setTag(R.id.row_id, downloadInfo.getRowId());
            holder.action.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    final long rowid = (long) view.getTag(R.id.row_id);
                    int status = (int) view.getTag(R.id.status);
                    if (status == DownloadManager.STATUS_RUNNING) {
                        cancel(rowid);
                    } else {
                        final PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
                        popupMenu.getMenuInflater().inflate(R.menu.menu_download, popupMenu.getMenu());
                        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem menuItem) {

                                switch (menuItem.getItemId()) {
                                    case R.id.remove:
                                        viewModel.removeDownload(rowid);
                                        TelemetryWrapper.downloadRemoveFile();
                                        popupMenu.dismiss();
                                        return true;
                                    case R.id.delete:
                                        delete(view, rowid);
                                        TelemetryWrapper.downloadDeleteFile();
                                        popupMenu.dismiss();
                                        return true;
                                    default:
                                        break;
                                }
                                return false;
                            }
                        });
                        popupMenu.show();
                    }
                    TelemetryWrapper.showFileContextMenu();
                }
            });

            holder.itemView.setTag(downloadInfo);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    final DownloadInfo download = (DownloadInfo) view.getTag();

                    if (download.getStatus() != DownloadManager.STATUS_SUCCESSFUL) {
                        return;
                    }

                    TelemetryWrapper.downloadOpenFile(false);

                    ThreadUtils.postToBackgroundThread(new Runnable() {
                        @Override
                        public void run() {
                            final boolean fileExist = new File(URI.create(download.getFileUri()).getPath()).exists();

                            ThreadUtils.postToMainThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (fileExist) {
                                        IntentUtils.intentOpenFile(view.getContext(), download.getFileUri(), download.getMimeType());
                                    } else {
                                        Toast.makeText(mContext, R.string.cannot_find_the_file, Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                        }
                    });
                }
            });

        } else if (viewHolder instanceof OnOpeningViewHolder) {
            viewHolder.itemView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        if (mDownloadInfo != null && !mDownloadInfo.isEmpty()) {
            return mDownloadInfo.size();
        } else {
            return 1;
        }
    }

    private String statusConvertStr(int status) {
        switch (status) {
            case DownloadManager.STATUS_PAUSED:
                return mContext.getResources().getString(R.string.pause);
            case DownloadManager.STATUS_PENDING:
                return mContext.getResources().getString(R.string.pending);
            case DownloadManager.STATUS_RUNNING:
                return mContext.getResources().getString(R.string.running);
            case DownloadManager.STATUS_SUCCESSFUL:
                return mContext.getResources().getString(R.string.successful);
            case DownloadManager.STATUS_FAILED:
                return mContext.getResources().getString(R.string.failed);
            default:
                return mContext.getResources().getString(R.string.unknown);
        }
    }

    public int mappingIcon(DownloadInfo downloadInfo) {

        if (SPECIFIC_FILE_EXTENSION.contains(downloadInfo.getFileExtension())) {
            return "apk".equals(downloadInfo.getFileExtension()) ? R.drawable.file_app : R.drawable.file_compressed;
        } else {

            if (!TextUtils.isEmpty(downloadInfo.getMimeType())) {
                String mimeType = downloadInfo.getMimeType().substring(0, downloadInfo.getMimeType().indexOf("/"));
                switch (mimeType) {
                    case "text":
                        return R.drawable.file_document;
                    case "image":
                        return R.drawable.file_image;
                    case "audio":
                        return R.drawable.file_music;
                    case "video":
                        return R.drawable.file_video;
                    default:
                        return R.drawable.file_document;
                }
            } else {
                return R.drawable.file_document;
            }
        }
    }

    public static class DownloadViewHolder extends RecyclerView.ViewHolder {

        @SuppressFBWarnings("URF_UNREAD_FIELD")
        ImageView icon;
        TextView title;
        TextView subtitle;
        ImageView action;
        ProgressBar progressBar;

        public DownloadViewHolder(View itemView) {
            super(itemView);
            icon = (ImageView) itemView.findViewById(R.id.img);
            title = (TextView) itemView.findViewById(R.id.title);
            subtitle = (TextView) itemView.findViewById(R.id.subtitle);
            action = itemView.findViewById(R.id.menu_action);
            progressBar = itemView.findViewById(R.id.progress);

        }
    }

    public static class DownloadEmptyViewHolder extends RecyclerView.ViewHolder {

        @SuppressFBWarnings("URF_UNREAD_FIELD")
        ImageView imag;

        public DownloadEmptyViewHolder(View itemView) {
            super(itemView);
            imag = (ImageView) itemView.findViewById(R.id.img);
        }
    }

    public static class OnOpeningViewHolder extends RecyclerView.ViewHolder {

        public OnOpeningViewHolder(View itemView) {
            super(itemView);
            itemView.setVisibility(View.GONE);
        }
    }

    private Snackbar getDeleteSnackBar(View view, final DownloadInfo deletedDownload) {
        final String deleteStr = mContext.getString(R.string.download_deleted, deletedDownload.getFileName());
        final File deleteFile = new File(URI.create(deletedDownload.getFileUri()).getPath());

        return Snackbar.make(view, deleteStr, Snackbar.LENGTH_SHORT)
                .addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>() {
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        super.onDismissed(transientBottomBar, event);

                        if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
                            try {
                                if (deleteFile.delete()) {
                                    //TODO move to view model is better, but we don't have context in DownloadInfoRepository.
                                    // Otherwise, this removal is not relevant to DownloadInfoLiveData
                                    DownloadManager manager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
                                    manager.remove(deletedDownload.getDownloadId());

                                    DownloadInfoManager.getInstance().delete(deletedDownload.getRowId(), null);
                                } else {
                                    Toast.makeText(mContext, R.string.cannot_delete_the_file, Toast.LENGTH_SHORT).show();
                                }

                            } catch (Exception e) {
                                Log.e(this.getClass().getSimpleName(), "" + e.getMessage());
                                Toast.makeText(mContext, R.string.cannot_delete_the_file, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onShown(Snackbar transientBottomBar) {
                        super.onShown(transientBottomBar);
                        viewModel.hideDownload(deletedDownload.getRowId());
                    }
                })
                .setAction(R.string.undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        viewModel.addDownload(deletedDownload);
                    }
                });
    }
}
