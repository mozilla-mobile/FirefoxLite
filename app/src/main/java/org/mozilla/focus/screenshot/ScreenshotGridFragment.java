/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.screenshot;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.mozilla.focus.R;
import org.mozilla.focus.fragment.PanelFragment;


public class ScreenshotGridFragment extends PanelFragment implements ScreenshotItemAdapter.StatusListener {

    private RecyclerView mContainerRecyclerView;
    private ViewGroup mContainerEmptyView;
    private ScreenshotItemAdapter mAdapter;

    public static ScreenshotGridFragment newInstance() {
        return new ScreenshotGridFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_screenshot_grid, container, false);

        mContainerRecyclerView = (RecyclerView) v.findViewById(R.id.screenshot_grid_recycler_view);
        mContainerEmptyView = (ViewGroup) v.findViewById(R.id.screenshot_grid_empty_view_container);

        TextView emptyText = (TextView) v.findViewById(R.id.screenshot_grid_empty_text);
        Drawable drawable = getResources().getDrawable(R.drawable.action_capture, null).mutate();
        drawable.setBounds(0, 0, getResources().getDimensionPixelSize(R.dimen.screenshot_empty_img_size), getResources().getDimensionPixelSize(R.dimen.screenshot_empty_img_size));
        DrawableCompat.setTint(drawable, ContextCompat.getColor(getActivity(), R.color.colorDownloadSubText));
        ImageSpan imageSpan = new ImageSpan(drawable);

        String emptyPrefix = getString(R.string.screenshot_grid_empty_text_msg_prefix);
        String emptyPostfix = getString(R.string.screenshot_grid_empty_text_msg_postfix);
        SpannableString spannableString = new SpannableString(emptyPrefix + emptyPostfix);

        int start = emptyPrefix.length();
        int end = start + 1;
        spannableString.setSpan(imageSpan, start, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        emptyText.setText(spannableString);

        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), 3);
        mAdapter = new ScreenshotItemAdapter(mContainerRecyclerView, getActivity(), this, layoutManager);
        mContainerRecyclerView.setLayoutManager(layoutManager);
        ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(3, getResources().getDimensionPixelSize(R.dimen.screenshot_grid_cell_padding));
        mContainerRecyclerView.addItemDecoration(itemDecoration);
        mContainerRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onStatus(@ViewStatus int status) {
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

    public void notifyItemDelete(long id) {
        if (mAdapter != null) {
            mAdapter.onItemDelete(id);
        }
    }

    @Override
    public void tryLoadMore() {
        mAdapter.tryLoadMore();
    }
}
