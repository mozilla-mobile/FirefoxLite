/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.mozilla.focus.Inject;
import org.mozilla.focus.R;
import org.mozilla.focus.widget.DownloadListAdapter;
import org.mozilla.rocket.download.DownloadInfoBundle;
import org.mozilla.rocket.download.DownloadInfoViewModel;

public class DownloadsFragment extends PanelFragment {

    private RecyclerView recyclerView;
    private DownloadListAdapter mDownloadListAdapter;

    public static DownloadsFragment newInstance() {
        return new DownloadsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        recyclerView = (RecyclerView) inflater.inflate(R.layout.fragment_downloads, container, false);

        final DownloadInfoViewModel viewModel = Inject.obtainDownloadInfoViewModel(getActivity());
        mDownloadListAdapter = new DownloadListAdapter(getContext(), viewModel);
        viewModel.getDownloadInfoBundle().observe(getViewLifecycleOwner(), downloadInfoBundle -> {
            if (downloadInfoBundle != null) {
                switch (downloadInfoBundle.getNotifyType()) {
                    case DownloadInfoBundle.Constants.NOTIFY_DATASET_CHANGED:
                        mDownloadListAdapter.setList(downloadInfoBundle.getList());
                        mDownloadListAdapter.notifyDataSetChanged();
                        break;
                    case DownloadInfoBundle.Constants.NOTIFY_ITEM_INSERTED:
                        mDownloadListAdapter.notifyItemInserted((int) downloadInfoBundle.getIndex());
                        break;
                    case DownloadInfoBundle.Constants.NOTIFY_ITEM_REMOVED:
                        mDownloadListAdapter.notifyItemRemoved((int) downloadInfoBundle.getIndex());
                        break;
                    case DownloadInfoBundle.Constants.NOTIFY_ITEM_CHANGED:
                        mDownloadListAdapter.notifyItemChanged((int) downloadInfoBundle.getIndex());
                        break;
                }
            }
        });

        return recyclerView;
    }

    @Override
    public void tryLoadMore() {
        Inject.obtainDownloadInfoViewModel(getActivity()).loadMore(false);
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
        Inject.obtainDownloadInfoViewModel(getActivity()).markAllItemsAreRead();
        super.onDestroy();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prepare();
    }

    private void prepare() {
        recyclerView.setAdapter(mDownloadListAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        // Update downloading progress via notifyItemChanged may cause row flashes so we disable item changed animation here
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

    }

}
