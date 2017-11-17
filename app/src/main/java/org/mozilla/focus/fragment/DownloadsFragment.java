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
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.mozilla.focus.R;
import org.mozilla.focus.download.DownloadInfo;
import org.mozilla.focus.download.DownloadInfoManager;
import org.mozilla.focus.widget.DownloadListAdapter;

import java.util.List;

public class DownloadsFragment extends PanelFragment implements DownloadInfoManager.AsyncQueryListener {

    private RecyclerView recyclerView;
    private DownloadListAdapter mDownloadListAdapter;
    private BroadcastReceiver mInsertReceiver;
    private BroadcastReceiver mDownloadReceiver;

    public static DownloadsFragment newInstance() {
        return new DownloadsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        recyclerView = (RecyclerView) inflater.inflate(R.layout.fragment_downloads, container, false);
        return recyclerView;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDownloadListAdapter = new DownloadListAdapter(getContext());
        mDownloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
                    long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L);
                    if (id > 0) {
                        DownloadInfoManager.getInstance().queryByDownloadId(id, DownloadsFragment.this);
                    }
                }
            }
        };

        mInsertReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(DownloadInfoManager.ROW_UPDATED)) {
                    Long id = intent.getLongExtra(DownloadInfoManager.ROW_ID, 0L);
                    if (id > 0) {
                        DownloadInfoManager.getInstance().queryByRowId(id, DownloadsFragment.this);
                    }
                }
            }
        };
    }

    @Override
    public void tryLoadMore() {
        if (!mDownloadListAdapter.isLoading() && !mDownloadListAdapter.isLastPage()) {
            mDownloadListAdapter.loadMore();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mInsertReceiver
                , new IntentFilter(DownloadInfoManager.ROW_UPDATED));
        getContext().registerReceiver(mDownloadReceiver
                , new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mInsertReceiver);
        getContext().unregisterReceiver(mDownloadReceiver);
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prepare();
    }

    private void prepare() {
        recyclerView.setAdapter(mDownloadListAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
    }

    @Override
    public void onQueryComplete(List downloadInfoList) {

        for (int i = 0; i < downloadInfoList.size(); i++) {
            DownloadInfo downloadInfo = (DownloadInfo) downloadInfoList.get(i);
            mDownloadListAdapter.updateItem(downloadInfo);
        }
    }
}
