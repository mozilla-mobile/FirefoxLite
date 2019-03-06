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
import android.widget.TextView;

import org.mozilla.focus.R;
import org.mozilla.focus.history.BrowsingHistoryFragment;
import org.mozilla.focus.screenshot.ScreenshotGridFragment;
import org.mozilla.focus.telemetry.TelemetryWrapper;

public class ListPanelDialog extends DialogFragment {

    public final static int TYPE_DEFAULT = 0;
    public final static int TYPE_DOWNLOADS = 1;
    public final static int TYPE_HISTORY = 2;
    public final static int TYPE_SCREENSHOTS = 3;
    public final static int TYPE_BOOKMARKS = 4;
    public final static int TYPE_NEWS = 5;


    private NestedScrollView scrollView;
    private static final String TYPE = "TYPE";
    private View bookmarksTouchArea;
    private View downloadsTouchArea;
    private View historyTouchArea;
    private View screenshotsTouchArea;
    private View divider;
    private View panelBottom;
    private TextView title;
    private boolean firstLaunch = true;
    private BottomSheetBehavior bottomSheetBehavior;

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
        showItem(getArguments().getInt(TYPE));
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
        title = v.findViewById(R.id.title);
        View bottomsheet = v.findViewById(R.id.bottom_sheet);
        scrollView = (NestedScrollView) v.findViewById(R.id.main_content);
        scrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {

            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                final int pageSize = v.getMeasuredHeight();
                // v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight() - scrollY is -49dp
                // When scrolled to end due to padding
                if (scrollY > oldScrollY && v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight() - scrollY < pageSize) {
                    final PanelFragment pf = (PanelFragment) getChildFragmentManager().findFragmentById(R.id.main_content);
                    if (pf != null && pf.isVisible()) {
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
        bottomSheetBehavior = BottomSheetBehavior.from(bottomsheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {

            private float translationY = Integer.MIN_VALUE;
            private int collapseHeight = -1;

            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    ListPanelDialog.this.dismissAllowingStateLoss();
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                float translationY = 0;

                if (slideOffset < 0) {
                    if (collapseHeight < 0) {
                        collapseHeight = bottomSheetBehavior.getPeekHeight();
                    }
                    translationY = collapseHeight * -slideOffset;
                }

                if (Float.compare(this.translationY, translationY) != 0) {
                    this.translationY = translationY;
                    divider.setTranslationY(translationY);
                    panelBottom.setTranslationY(translationY);
                }
            }
        });
        v.findViewById(R.id.container).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissAllowingStateLoss();
            }
        });
        divider = v.findViewById(R.id.divider);
        panelBottom = v.findViewById(R.id.panel_bottom);
        bookmarksTouchArea = v.findViewById(R.id.bookmarks);
        bookmarksTouchArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showItem(TYPE_BOOKMARKS);
                TelemetryWrapper.showPanelBookmark();
            }
        });
        downloadsTouchArea = v.findViewById(R.id.downloads);
        downloadsTouchArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showItem(TYPE_DOWNLOADS);
                TelemetryWrapper.showPanelDownload();
            }
        });
        historyTouchArea = v.findViewById(R.id.history);
        historyTouchArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showItem(TYPE_HISTORY);
                TelemetryWrapper.showPanelHistory();
            }
        });
        screenshotsTouchArea = v.findViewById(R.id.screenshots);
        screenshotsTouchArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showItem(TYPE_SCREENSHOTS);
                TelemetryWrapper.showPanelCapture();
            }
        });
        return v;
    }

    private void setSelectedItem(int selectedItem) {
        getArguments().putInt(TYPE, selectedItem);
        toggleSelectedItem();
    }

    private void showItem(int type) {
        if (firstLaunch || getArguments().getInt(TYPE) != type) {
            title.setText(getTitle(type));
            setSelectedItem(type);
            showPanelFragment(createFragmentByType(type));
        }
    }

    private int getTitle(int type) {
        switch (type) {
            default:
            case TYPE_DOWNLOADS:
                return R.string.label_menu_download;
            case TYPE_HISTORY:
                return R.string.label_menu_history;
            case TYPE_SCREENSHOTS:
                return R.string.label_menu_my_shots;
            case TYPE_BOOKMARKS:
                return R.string.label_menu_bookmark;
            case TYPE_NEWS:
                return R.string.label_menu_news;
        }
    }

    private PanelFragment createFragmentByType(int type) {
        switch (type) {
            default:
            case TYPE_DOWNLOADS:
                return DownloadsFragment.newInstance();
            case TYPE_HISTORY:
                return BrowsingHistoryFragment.newInstance();
            case TYPE_SCREENSHOTS:
                return ScreenshotGridFragment.newInstance();
            case TYPE_BOOKMARKS:
                return BookmarksFragment.newInstance();
            case TYPE_NEWS:
                return NewsFragment.newInstance();
        }
    }

    private void showPanelFragment(PanelFragment panelFragment) {
        getChildFragmentManager().beginTransaction().replace(R.id.main_content, panelFragment).commit();
    }

    private void toggleSelectedItem() {
        firstLaunch = false;
        bookmarksTouchArea.setSelected(false);
        downloadsTouchArea.setSelected(false);
        historyTouchArea.setSelected(false);
        screenshotsTouchArea.setSelected(false);
        switch (getArguments().getInt(TYPE)) {
            case TYPE_BOOKMARKS:
                bookmarksTouchArea.setSelected(true);
                break;
            case TYPE_DOWNLOADS:
                downloadsTouchArea.setSelected(true);
                break;
            case TYPE_HISTORY:
                historyTouchArea.setSelected(true);
                break;
            case TYPE_SCREENSHOTS:
                screenshotsTouchArea.setSelected(true);
                break;
            case TYPE_NEWS:
                divider.setVisibility(View.GONE);
                panelBottom.setVisibility(View.GONE);
                scrollView.setPadding(0, 0, 0, 0);
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                break;
            default:
                throw new RuntimeException("There is no view type " + getArguments().getInt(TYPE));
        }
    }
}
