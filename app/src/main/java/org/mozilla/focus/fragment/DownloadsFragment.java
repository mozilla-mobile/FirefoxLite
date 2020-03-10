/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import org.mozilla.focus.R;
import org.mozilla.focus.download.DownloadInfoManager;
import org.mozilla.focus.widget.DownloadListAdapter;
import org.mozilla.rocket.content.BaseViewModelFactory;
import org.mozilla.rocket.content.ExtentionKt;
import org.mozilla.rocket.download.DownloadIndicatorViewModel;
import org.mozilla.rocket.download.DownloadInfoPack;
import org.mozilla.rocket.download.DownloadInfoViewModel;

import javax.inject.Inject;

import dagger.Lazy;

public class DownloadsFragment extends PanelFragment implements DownloadInfoViewModel.OnProgressUpdateListener {

    private static final int MSG_UPDATE_PROGRESS = 1;
    private static final int QUERY_PROGRESS_DELAY = 500;

    @Inject
    Lazy<DownloadInfoViewModel> downloadInfoViewModelCreator;

    @Inject
    Lazy<DownloadIndicatorViewModel> downloadIndicatorViewModelCreator;

    private RecyclerView recyclerView;
    private DownloadListAdapter downloadListAdapter;
    private DownloadInfoViewModel viewModel;
    private HandlerThread handlerThread;
    private Handler handler;

    public static DownloadsFragment newInstance() {
        return new DownloadsFragment();
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L);
                if (id > 0) {
                    viewModel.notifyDownloadComplete(id);
                }
            } else if (DownloadInfoManager.ROW_UPDATED.equals(intent.getAction())) {
                long id = intent.getLongExtra(DownloadInfoManager.ROW_ID, 0L);
                if (id > 0) {
                    viewModel.notifyRowUpdate(id);
                }
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        ExtentionKt.appComponent(this).inject(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        recyclerView = (RecyclerView) inflater.inflate(R.layout.fragment_downloads, container, false);

        viewModel = new ViewModelProvider(requireActivity(), new BaseViewModelFactory<>(downloadInfoViewModelCreator::get)).get(DownloadInfoViewModel.class);
        downloadListAdapter = new DownloadListAdapter(getContext(), viewModel);
        viewModel.getDownloadInfoObservable().observe(getViewLifecycleOwner(), downloadInfoPack -> {
            if (downloadInfoPack != null) {
                switch (downloadInfoPack.getNotifyType()) {
                    case DownloadInfoPack.Constants.NOTIFY_DATASET_CHANGED:
                        downloadListAdapter.setList(downloadInfoPack.getList());
                        downloadListAdapter.notifyDataSetChanged();
                        break;
                    case DownloadInfoPack.Constants.NOTIFY_ITEM_INSERTED:
                        downloadListAdapter.notifyItemInserted((int) downloadInfoPack.getIndex());
                        break;
                    case DownloadInfoPack.Constants.NOTIFY_ITEM_REMOVED:
                        downloadListAdapter.notifyItemRemoved((int) downloadInfoPack.getIndex());
                        break;
                    case DownloadInfoPack.Constants.NOTIFY_ITEM_CHANGED:
                        downloadListAdapter.notifyItemChanged((int) downloadInfoPack.getIndex());
                        break;
                }
            }
        });
        viewModel.getToastMessageObservable().observe(getViewLifecycleOwner(), stringId -> {
            final String msg = getString(stringId);
            Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
        });
        viewModel.getDeleteSnackbarObservable().observe(getViewLifecycleOwner(), downloadInfo -> {
            final String deleteStr = getString(R.string.download_deleted, downloadInfo.getFileName());
            Snackbar.make(recyclerView, deleteStr, Snackbar.LENGTH_SHORT)
                    .addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>() {
                        @Override
                        public void onDismissed(Snackbar transientBottomBar, int event) {
                            super.onDismissed(transientBottomBar, event);

                            if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
                                viewModel.confirmDelete(downloadInfo);
                            }
                        }

                        @Override
                        public void onShown(Snackbar transientBottomBar) {
                            super.onShown(transientBottomBar);
                            viewModel.hide(downloadInfo.getRowId());
                        }
                    })
                    .setAction(R.string.undo, view -> viewModel.add(downloadInfo)).show();
        });

        return recyclerView;
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
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(broadcastReceiver, new IntentFilter(DownloadInfoManager.ROW_UPDATED));
        getActivity().registerReceiver(broadcastReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        viewModel.registerForProgressUpdate(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
        getActivity().unregisterReceiver(broadcastReceiver);
        viewModel.unregisterForProgressUpdate();

    }

    @Override
    public void onDestroy() {
        // mark all items are unread when leave the download panel
        viewModel.markAllItemsAreRead();
        // When download indicator is showing and download is failed, we won't get notified by DownloadManager. Then back to BrowserFragment/HomeFragment will not
        // go through fragment's onResume i.e. LiveData's onActive. So force trigger download indicator update here.
        new ViewModelProvider(requireActivity(), new BaseViewModelFactory<>(downloadIndicatorViewModelCreator::get)).get(DownloadIndicatorViewModel.class)
                .updateIndicator();
        cleanUp();
        super.onDestroy();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prepare();
    }

    private void prepare() {
        recyclerView.setAdapter(downloadListAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), RecyclerView.VERTICAL, false));
        // Update downloading progress via notifyItemChanged may cause row flashes so we disable item changed animation here
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

    }

    @Override
    public void onStartUpdate() {
        startProgressUpdate();
    }

    @Override
    public void onCompleteUpdate() {
        if (handler != null && !handler.hasMessages(MSG_UPDATE_PROGRESS)) {
            handler.sendEmptyMessageDelayed(MSG_UPDATE_PROGRESS, QUERY_PROGRESS_DELAY);
        }
    }

    @Override
    public void onStopUpdate() {
        if (handler != null) {
            handler.removeMessages(MSG_UPDATE_PROGRESS);
        }
    }

    private void startProgressUpdate() {
        if (handlerThread == null) {
            handlerThread = new HandlerThread("download-progress");
            handlerThread.start();
            handler = new Handler(handlerThread.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    if (msg.what == MSG_UPDATE_PROGRESS) {
                        viewModel.queryDownloadProgress();
                    }
                }
            };
        }
        if (!handler.hasMessages(MSG_UPDATE_PROGRESS)) {
            handler.sendEmptyMessage(MSG_UPDATE_PROGRESS);
        }
    }

    private void cleanUp() {
        if (handlerThread != null) {
            handlerThread.quit();
            handlerThread = null;
            handler = null;
        }
    }

}
