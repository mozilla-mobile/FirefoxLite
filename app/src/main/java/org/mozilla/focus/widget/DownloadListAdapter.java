/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget;

import android.app.DownloadManager;
import android.content.Context;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import org.mozilla.focus.R;
import org.mozilla.focus.download.DownloadInfo;
import org.mozilla.focus.download.DownloadInfoManager;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.utils.IntentUtils;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Created by anlin on 01/08/2017.
 */

public class DownloadListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final List<String> SPECIFIC_FILE_EXTENSION
            = Arrays.asList("apk", "zip", "gz", "tar", "7z", "rar", "war");
    private List<DownloadInfo> mDownloadInfo;
    private static final int VIEW_TYPE_EMPTY = 0;
    private static final int VIEW_TYPE_NON_EMPTY = 1;
    private static final int ON_OPENING = 2;
    private static final int PAGE_SIZE = 20;
    private Context mContext;
    private int mItemCount = 0;
    private boolean isOpening = false;
    private boolean isLoading = false;
    private boolean isLastPage = false;

    public DownloadListAdapter(Context context) {
        mContext = context;
        mDownloadInfo = new ArrayList<>();
        loadMore();
        isOpening = true;
    }

    public boolean isLoading() {
        return isLoading;
    }

    public boolean isLastPage() {
        return isLastPage;
    }

    public void loadMore() {
        DownloadInfoManager.getInstance().query(mItemCount, PAGE_SIZE, new DownloadInfoManager.AsyncQueryListener() {
            @Override
            public void onQueryComplete(List downloadInfoList) {

                mDownloadInfo.addAll(downloadInfoList);
                mItemCount = mDownloadInfo.size();
                notifyDataSetChanged();
                isOpening = false;
                isLoading = false;
                isLastPage = downloadInfoList.size() == 0;
            }
        });
        isLoading = true;
    }

    public void updateItem(DownloadInfo downloadInfo) {
        int index = -1;
        for (int i = 0; i < mDownloadInfo.size(); i++) {
            if (mDownloadInfo.get(i).getRowId().equals(downloadInfo.getRowId())) {
                index = i;
                break;
            }
        }

        if (index == -1) {
            mDownloadInfo.add(0, downloadInfo);
        } else {
            mDownloadInfo.remove(index);
            mDownloadInfo.add(index, downloadInfo);
        }
        this.notifyDataSetChanged();
    }

    private void removeItem(long rowId) {
        DownloadInfoManager.getInstance().delete(rowId, null);
        hideItem(rowId);
    }

    private void delete(final View view, final long rowId) {
        DownloadInfoManager.getInstance().queryByRowId(rowId, new DownloadInfoManager.AsyncQueryListener() {
            @Override
            public void onQueryComplete(List downloadInfoList) {
                if (downloadInfoList.size() > 0
                        && rowId == ((DownloadInfo) downloadInfoList.get(0)).getRowId()) {

                    DownloadInfo deletedDownload = (DownloadInfo) downloadInfoList.get(0);
                    File file = new File(URI.create(deletedDownload.getFileUri()).getPath());

                    Snackbar snackBar = getDeleteSnackBar(view, deletedDownload);

                    if (file.exists()) {
                        snackBar.show();
                    } else {
                        Toast.makeText(mContext, R.string.cannot_find_the_file, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private void cancel(final long rowId) {

        DownloadInfoManager.getInstance().queryByRowId(rowId, new DownloadInfoManager.AsyncQueryListener() {
            @Override
            public void onQueryComplete(List downloadInfoList) {
                if (downloadInfoList.size() > 0) {
                    DownloadInfo downloadInfo = (DownloadInfo) downloadInfoList.get(0);

                    if (!downloadInfo.existInDownloadManager()) {
                        return;
                    }

                    if ((rowId == downloadInfo.getRowId())
                            && (DownloadManager.STATUS_SUCCESSFUL != downloadInfo.getStatus())) {

                        String cancelStr = mContext.getString(R.string.download_cancel);
                        Toast.makeText(mContext, cancelStr, Toast.LENGTH_SHORT).show();

                        DownloadManager manager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
                        manager.remove(downloadInfo.getDownloadId());

                        removeItem(rowId);
                    }

                }
            }
        });
    }

    private void addItem(DownloadInfo downloadInfo) {
        int index = -1;
        for (int i = 0; i < mDownloadInfo.size(); i++) {
            if (mDownloadInfo.get(i).getRowId() < downloadInfo.getRowId()) {
                index = i;
                break;
            }
        }

        if (index == -1) {
            mDownloadInfo.add(downloadInfo);
            //The crash will happen when data set size is 1 after add item.
            //Because we define item count is 1 and mDownloadInfo is empty that means nothing and show empty view.
            //So use notifyDataSetChanged() instead of notifyItemInserted when data size is 1 after add item.
            if (mDownloadInfo.size() > 1) {
                this.notifyItemInserted(mDownloadInfo.size() - 1);
            } else {
                this.notifyDataSetChanged();
            }

        } else {
            mDownloadInfo.add(index, downloadInfo);
            this.notifyItemInserted(index);
        }
    }

    private void hideItem(long rowId) {
        for (int i = 0; i < mDownloadInfo.size(); i++) {
            DownloadInfo downloadInfo = mDownloadInfo.get(i);
            if (rowId == downloadInfo.getRowId()) {
                mDownloadInfo.remove(downloadInfo);
                this.notifyItemRemoved(i);
                break;
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (isOpening) {
            return ON_OPENING;
        } else {
            if (mDownloadInfo.isEmpty()) {
                return VIEW_TYPE_EMPTY;
            } else {
                return VIEW_TYPE_NON_EMPTY;
            }
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View itemView;
        if (VIEW_TYPE_NON_EMPTY == viewType) {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.download_menu_cell, parent, false);
            return new DownloadViewHolder(itemView);
        } else if (ON_OPENING == viewType) {
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
            } else {
                subtitle = statusConvertStr(downloadInfo.getStatus());
            }

            holder.subtitle.setText(subtitle);

            holder.action.setTag(R.id.status, downloadInfo.getStatus());
            holder.action.setTag(R.id.row_id, downloadInfo.getRowId());
            holder.action.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    final long rowid = (long) view.getTag(R.id.row_id);
                    int status = (int) view.getTag(R.id.status);
                    final PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
                    popupMenu.getMenuInflater().inflate(R.menu.menu_delete, popupMenu.getMenu());
                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {

                            switch (menuItem.getItemId()) {
                                case R.id.remove:
                                    removeItem(rowid);
                                    TelemetryWrapper.downloadRemoveFile();
                                    popupMenu.dismiss();
                                    return true;
                                case R.id.delete:
                                    delete(view, rowid);
                                    TelemetryWrapper.downloadDeleteFile();
                                    popupMenu.dismiss();
                                    return true;
                                case R.id.cancel:
                                    cancel(rowid);
                                    popupMenu.dismiss();
                                    return true;
                                default:
                                    break;
                            }
                            return false;
                        }
                    });


                    if (DownloadManager.STATUS_RUNNING == status) {

                        popupMenu.getMenu().findItem(R.id.remove).setVisible(false);
                        popupMenu.getMenu().findItem(R.id.delete).setVisible(false);
                        popupMenu.getMenu().findItem(R.id.cancel).setVisible(true);

                    } else {
                        popupMenu.getMenu().findItem(R.id.remove).setVisible(true);
                        popupMenu.getMenu().findItem(R.id.delete).setVisible(true);
                        popupMenu.getMenu().findItem(R.id.cancel).setVisible(false);
                    }

                    popupMenu.show();
                    TelemetryWrapper.showFileContextMenu();
                }
            });

            holder.itemView.setTag(downloadInfo);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    DownloadInfo download = (DownloadInfo) view.getTag();

                    TelemetryWrapper.downloadOpenFile(false);

                    if (new File(URI.create(download.getFileUri()).getPath()).exists()) {
                        IntentUtils.intentOpenFile(view.getContext(), download.getFileUri(), download.getMimeType());
                    } else {
                        Toast.makeText(mContext, R.string.cannot_find_the_file, Toast.LENGTH_LONG).show();
                    }
                }
            });

        } else if (viewHolder instanceof OnOpeningViewHolder) {
            viewHolder.itemView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        if (!mDownloadInfo.isEmpty()) {
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
        FrameLayout action;

        public DownloadViewHolder(View itemView) {
            super(itemView);
            icon = (ImageView) itemView.findViewById(R.id.img);
            title = (TextView) itemView.findViewById(R.id.title);
            subtitle = (TextView) itemView.findViewById(R.id.subtitle);
            action = (FrameLayout) itemView.findViewById(R.id.menu_action);

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
                        hideItem(deletedDownload.getRowId());
                    }
                })
                .setAction(R.string.undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        addItem(deletedDownload);
                    }
                });
    }
}
