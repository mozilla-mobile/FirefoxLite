/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.DialogFragment;
import android.support.v4.widget.NestedScrollView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.mozilla.focus.R;
import org.mozilla.focus.history.BrowsingHistoryFragment;
import org.mozilla.focus.screenshot.ScreenshotGridFragment;
import org.mozilla.focus.telemetry.TelemetryWrapper;

public class ListPanelDialog extends DialogFragment {

    public final static int TYPE_DOWNLOADS = 1;
    public final static int TYPE_HISTORY = 2;
    public final static int TYPE_SCREENSHOTS = 3;
    private static final int UNUSED_REQUEST_CODE = -1;

    private NestedScrollView scrollView;
    private static final String TYPE = "TYPE";
    private View downloadsTouchArea;
    private View historyTouchArea;
    private View screenshotsTouchArea;
    private boolean firstLaunch = true;

    public static ListPanelDialog newInstance(int type) {
        ListPanelDialog listPanelDialog = new ListPanelDialog();
        Bundle args = new Bundle();
        args.putInt(TYPE, type);
        listPanelDialog.setArguments(args);
        return listPanelDialog;
    }

    @Override
    public void onResume() {
        super.onResume();
        switch (getArguments().getInt(TYPE)) {
            case TYPE_DOWNLOADS:
                showDownloads();
                break;
            case TYPE_HISTORY:
                showHistory();
                break;
            case TYPE_SCREENSHOTS:
                showScreenshots();
                break;
            default:
                throw new RuntimeException("There is no view type " + getArguments().getInt(TYPE));
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.BottomSheetTheme);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_listpanel_dialog, container, false);
        scrollView = (NestedScrollView) v.findViewById(R.id.main_content);
        scrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {

            private static final long REQUEST_THROTTLE_THRESHOLD = 3000;
            private long lastRequestTime = 0;

            private boolean detectThrottle() {
                long now = System.currentTimeMillis();
                boolean throttled = now - lastRequestTime < REQUEST_THROTTLE_THRESHOLD;
                lastRequestTime = now;
                return throttled;
            }

            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {

                final int pageSize = v.getMeasuredHeight();
                // v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight() - scrollY is -49dp
                // When scrolled to end due to padding
                if ( scrollY > oldScrollY && v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight() - scrollY < 3 * pageSize ) {
                    if (detectThrottle()) {
                        return;
                    }
                    final PanelFragment pf = (PanelFragment) getChildFragmentManager().findFragmentById(R.id.main_content);
                    if ( pf != null && pf.isVisible() ) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                pf.tryLoadMore();
                            }
                        }).start();
                    }
                }
            }
        });
        BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(scrollView);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    ListPanelDialog.this.dismiss();
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });
        v.findViewById(R.id.container).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        downloadsTouchArea = v.findViewById(R.id.downloads);
        downloadsTouchArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDownloads();
                TelemetryWrapper.showPanelDownload();
            }
        });
        historyTouchArea = v.findViewById(R.id.history);
        historyTouchArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHistory();
                TelemetryWrapper.showPanelHistory();
            }
        });
        screenshotsTouchArea = v.findViewById(R.id.screenshots);
        screenshotsTouchArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showScreenshots();
                TelemetryWrapper.showPanelCapture();
            }
        });
        return v;
    }

    private void setSelectedItem(int selectedItem) {
        getArguments().putInt(TYPE, selectedItem);
        toggleSelectedItem();
    }

    private void showDownloads() {
        if(firstLaunch || getArguments().getInt(TYPE) != TYPE_DOWNLOADS) {
            setSelectedItem(TYPE_DOWNLOADS);
            showPanelFragment(DownloadsFragment.newInstance());
        }
    }

    private void showHistory() {
        if(firstLaunch || getArguments().getInt(TYPE) != TYPE_HISTORY) {
            setSelectedItem(TYPE_HISTORY);
            showPanelFragment(BrowsingHistoryFragment.newInstance());
        }
    }

    private void showScreenshots() {
        if (firstLaunch || getArguments().getInt(TYPE) != TYPE_SCREENSHOTS) {
            setSelectedItem(TYPE_SCREENSHOTS);
            showPanelFragment(ScreenshotGridFragment.newInstance());
        }
    }

    private void showPanelFragment(PanelFragment panelFragment) {
        getChildFragmentManager().beginTransaction().replace(R.id.main_content, panelFragment).commit();
    }

    private void toggleSelectedItem() {
        firstLaunch = false;
        downloadsTouchArea.setSelected(false);
        historyTouchArea.setSelected(false);
        screenshotsTouchArea.setSelected(false);
        switch (getArguments().getInt(TYPE)) {
            case TYPE_DOWNLOADS:
                downloadsTouchArea.setSelected(true);
                break;
            case TYPE_HISTORY:
                historyTouchArea.setSelected(true);
                break;
            case TYPE_SCREENSHOTS:
                screenshotsTouchArea.setSelected(true);
                break;
            default:
                throw new RuntimeException("There is no view type " + getArguments().getInt(TYPE));
        }
    }
}
