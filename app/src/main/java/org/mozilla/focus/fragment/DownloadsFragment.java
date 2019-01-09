/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment;

import android.app.DownloadManager;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.PopupMenu;
import android.widget.Toast;
import org.mozilla.focus.Inject;
import org.mozilla.focus.R;
import org.mozilla.focus.download.DownloadInfo;
import org.mozilla.focus.download.DownloadInfoManager;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.widget.DownloadListAdapter;
import org.mozilla.rocket.download.DownloadInfoBundle;
import org.mozilla.rocket.download.DownloadInfoViewModel;

import javax.annotation.Nullable;
import java.io.File;
import java.net.URI;

public class DownloadsFragment extends PanelFragment implements DownloadListAdapter.DownloadPanelListener {

    @Nullable
    DownloadInfoViewModel viewModel;

    public static DownloadsFragment newInstance() {
        return new DownloadsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        RecyclerView recyclerView = (RecyclerView) inflater.inflate(R.layout.fragment_downloads, container, false);
        initRecyclerView(recyclerView);

        DownloadListAdapter downloadListAdapter = new DownloadListAdapter(getContext(), this);
        recyclerView.setAdapter(downloadListAdapter);

        subscribeUI(downloadListAdapter);

        return recyclerView;
    }

    private void initRecyclerView(RecyclerView recyclerView) {
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        // Update downloading progress via notifyItemChanged may cause row flashes so we disable item changed animation here
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
    }

    private void subscribeUI(DownloadListAdapter downloadListAdapter) {
        viewModel = Inject.obtainDownloadInfoViewModel(getActivity());
        viewModel.getDownloadInfoBundle().observe(this, downloadInfoBundle -> {
                switch (downloadInfoBundle.getNotifyType()) {
                    case DownloadInfoBundle.Constants.NOTIFY_DATASET_CHANGED:
                        downloadListAdapter.setList(downloadInfoBundle.getList());
                        downloadListAdapter.notifyDataSetChanged();
                        break;
                    case DownloadInfoBundle.Constants.NOTIFY_ITEM_INSERTED:
                        downloadListAdapter.notifyItemInserted((int) downloadInfoBundle.getIndex());
                        break;
                    case DownloadInfoBundle.Constants.NOTIFY_ITEM_REMOVED:
                        downloadListAdapter.notifyItemRemoved((int) downloadInfoBundle.getIndex());
                        break;
                    case DownloadInfoBundle.Constants.NOTIFY_ITEM_CHANGED:
                        downloadListAdapter.notifyItemChanged((int) downloadInfoBundle.getIndex());
                        break;
                }

        });
        viewModel.loadMore(true);
        viewModel.setOpening(true);
    }

    @Override
    public void tryLoadMore() {
        viewModel.loadMore(false);
    }

    // TODO: 6/26/18 Currently DownloadsFragment handles everything in RecyclerView so it does
    // nothing here. This is planned to be changed.
    @Override
    public void onStatus(int status) {
        // Do nothing.
    }

    @Override
    public void onDestroy() {
        // mark all items are unread when leave the download panel
        viewModel.markAllItemsAreRead();
        // When download indicator is showing and download is failed, we won't get notified by DownloadManager. Then back to BrowserFragment/HomeFragment will not
        // go through fragment's onResume i.e. LiveData's onActive. So force trigger download indicator update here.
        Inject.obtainDownloadIndicatorViewModel(getActivity()).updateIndicator();
        super.onDestroy();
    }


    void delete(final View view, final long rowId) {
        viewModel.delete(rowId, downloadInfo -> {
            final File file = new File(URI.create(downloadInfo.getFileUri()).getPath());
            if (file.exists()) {
                final Snackbar snackBar = getDeleteSnackBar(view, downloadInfo);
                snackBar.show();
            } else {
                Toast.makeText(getActivity(), R.string.cannot_find_the_file, Toast.LENGTH_SHORT).show();
            }
        });
    }

    void cancel(final long rowId) {
        viewModel.cancelDownload(rowId, downloadInfo -> {
            final String cancelStr = getString(R.string.download_cancel);
            Toast.makeText(getActivity(), cancelStr, Toast.LENGTH_SHORT).show();
        });
    }

    private Snackbar getDeleteSnackBar(View view, final DownloadInfo deletedDownload) {
        final String deleteStr = getActivity().getString(R.string.download_deleted, deletedDownload.getFileName());
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
                                    DownloadManager manager = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
                                    manager.remove(deletedDownload.getDownloadId());

                                    DownloadInfoManager.getInstance().delete(deletedDownload.getRowId(), null);
                                } else {
                                    Toast.makeText(getActivity(), R.string.cannot_delete_the_file, Toast.LENGTH_SHORT).show();
                                }

                            } catch (Exception e) {
                                Log.e(this.getClass().getSimpleName(), "" + e.getMessage());
                                Toast.makeText(getActivity(), R.string.cannot_delete_the_file, Toast.LENGTH_SHORT).show();
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

    @Override
    public View.OnClickListener provideOnClickListener() {
        return view -> {
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
        };
    }

    @Override
    public boolean isOpening() {
        return viewModel.isOpening();
    }
}
