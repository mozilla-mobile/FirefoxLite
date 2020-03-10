/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.mozilla.focus.R;
import org.mozilla.focus.activity.EditBookmarkActivity;
import org.mozilla.focus.activity.EditBookmarkActivityKt;
import org.mozilla.focus.bookmark.BookmarkAdapter;
import org.mozilla.focus.navigation.ScreenNavigator;
import org.mozilla.focus.persistence.BookmarkModel;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.viewmodel.BookmarkViewModel;
import org.mozilla.rocket.content.BaseViewModelFactory;
import org.mozilla.rocket.content.ExtentionKt;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import dagger.Lazy;

public class BookmarksFragment extends PanelFragment implements BookmarkAdapter.BookmarkPanelListener {

    @Inject
    Lazy<BookmarkViewModel> bookmarkViewModelCreator;

    private RecyclerView recyclerView;
    private View emptyView;
    private BookmarkAdapter adapter;
    private BookmarkViewModel viewModel;

    public static BookmarksFragment newInstance() {
        return new BookmarksFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        ExtentionKt.appComponent(this).inject(this);
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity(), new BaseViewModelFactory<>(bookmarkViewModelCreator::get)).get(BookmarkViewModel.class);
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
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        adapter = new BookmarkAdapter(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(layoutManager);

        viewModel.getBookmarks().observe(getViewLifecycleOwner(), bookmarks -> adapter.setData(bookmarks));

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
    public void onItemClicked(String url) {
        ScreenNavigator.get(getContext()).showBrowserScreen(url, true, false);
        closePanel();
        TelemetryWrapper.bookmarkOpenItem();
    }

    @Override
    public void onItemDeleted(BookmarkModel bookmark) {
        viewModel.deleteBookmark(bookmark);
        TelemetryWrapper.bookmarkRemoveItem();
    }

    @Override
    public void onItemEdited(BookmarkModel bookmark) {
        startActivity(new Intent(getContext(), EditBookmarkActivity.class).putExtra(EditBookmarkActivityKt.ITEM_UUID_KEY, bookmark.getId()));
        TelemetryWrapper.bookmarkEditItem();
    }
}
