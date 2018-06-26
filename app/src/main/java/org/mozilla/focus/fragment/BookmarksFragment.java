/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.mozilla.focus.R;
import org.mozilla.focus.bookmark.BookmarkAdapter;
import org.mozilla.focus.history.HistoryItemAdapter;

import javax.annotation.Nonnull;


public class BookmarksFragment extends PanelFragment implements ItemClosingPanelFragmentStatusListener {
    private RecyclerView recyclerView;
    private View emptyView;
    private BookmarkAdapter mAdapter;

    public static BookmarksFragment newInstance() {
        return new BookmarksFragment();
    }

    @Override
    public View onCreateView(@Nonnull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_bookmarks, container, false);
        recyclerView = v.findViewById(R.id.recyclerview);
        emptyView = v.findViewById(R.id.empty_view_container);
        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mAdapter = new BookmarkAdapter(getContext(), this);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(layoutManager);
        onStatus(VIEW_TYPE_NON_EMPTY);
    }

    @Override
    public void tryLoadMore() {
        // Do nothing for now.
    }

    @Override
    public void onStatus(int status) {
        if (VIEW_TYPE_EMPTY == status) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else if (VIEW_TYPE_NON_EMPTY == status) {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        } else {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onItemClicked() {
        closePanel();
    }
}
