/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.history;

import android.content.Context;
import android.content.DialogInterface;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.mozilla.focus.R;
import org.mozilla.focus.fragment.PanelFragment;
import org.mozilla.focus.home.HomeFragment;
import org.mozilla.focus.utils.TopSitesUtils;
import org.mozilla.focus.telemetry.TelemetryWrapper;


public class BrowsingHistoryFragment extends PanelFragment implements View.OnClickListener, HistoryItemAdapter.HistoryListener {

    public static final int VIEW_TYPE_EMPTY = 0;
    public static final int VIEW_TYPE_NON_EMPTY = 1;
    public static final int ON_OPENING = 2;
    private RecyclerView mRecyclerView;
    private ViewGroup mContainerEmptyView, mContainerRecyclerView;
    private HistoryItemAdapter mAdapter;

    public static BrowsingHistoryFragment newInstance() {
        return new BrowsingHistoryFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_browsing_history, container, false);
        v.findViewById(R.id.browsing_history_btn_clear).setOnClickListener(this);

        mContainerRecyclerView = (ViewGroup) v.findViewById(R.id.browsing_history_recycler_view_container);
        mContainerEmptyView = (ViewGroup) v.findViewById(R.id.browsing_history_empty_view_container);
        mRecyclerView = (RecyclerView) v.findViewById(R.id.browsing_history_recycler_view);
        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mAdapter = new HistoryItemAdapter(mRecyclerView, getActivity(), this, layoutManager);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(layoutManager);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.browsing_history_btn_clear:
                // if Fragment is detached but AlertDialog still on the screen, we might get null context in callback
                final Context ctx = getContext();
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle);
                builder.setTitle(R.string.browsing_history_dialog_confirm_clear_message);
                builder.setPositiveButton(R.string.browsing_history_dialog_btn_clear, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (ctx == null) {
                            return;
                        }
                        mAdapter.clear();
                        TopSitesUtils.getDefaultSitesJsonArrayFromAssets(ctx);
                        final Fragment fragment = getParentFragment().getTargetFragment();
                        if (fragment != null && fragment instanceof HomeFragment) {
                            fragment.onActivityResult(HomeFragment.REFRESH_REQUEST_CODE, Activity.RESULT_OK, null);
                        }
                        TelemetryWrapper.clearHistory();
                    }
                });
                builder.setNegativeButton(R.string.action_cancel, null);
                builder.create().show();
                break;
            default:
                break;
        }
    }

    @Override
    public void onStatus(int status) {
        if (VIEW_TYPE_EMPTY == status) {
            mContainerRecyclerView.setVisibility(View.GONE);
            mContainerEmptyView.setVisibility(View.VISIBLE);
        } else if (VIEW_TYPE_NON_EMPTY == status) {
            mContainerRecyclerView.setVisibility(View.VISIBLE);
            mContainerEmptyView.setVisibility(View.GONE);
        } else {
            mContainerRecyclerView.setVisibility(View.GONE);
            mContainerEmptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onItemClicked() {
        closePanel();
    }
}
