/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment;

import android.content.DialogInterface;
import android.graphics.Outline;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import org.mozilla.focus.R;
import org.mozilla.focus.history.BrowsingHistoryFragment;
import org.mozilla.focus.screenshot.ScreenshotGridFragment;
import org.mozilla.focus.telemetry.TelemetryWrapper;

public class ListPanelDialog extends DialogFragment {

    public final static int TYPE_DOWNLOADS = 1;
    public final static int TYPE_HISTORY = 2;
    public final static int TYPE_SCREENSHOTS = 3;
    public final static int TYPE_BOOKMARKS = 4;

    private NestedScrollView scrollView;
    private static final String TYPE = "TYPE";
    private View bookmarksIcon;
    private View downloadsIcon;
    private View historyIcon;
    private View screenshotsIcon;
    private View bookmarksSelectedIcon;
    private View downloadsSelectedIcon;
    private View historySelectedIcon;
    private View screenshotsSelectedIcon;
    private TextView title;
    private boolean firstLaunch = true;
    private BottomSheetBehavior bottomSheetBehavior;

    private DialogInterface.OnDismissListener onDismissListener;

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
        View contentLayout = v.findViewById(R.id.container);
        final float cornerRadius = getResources().getDimension(R.dimen.menu_corner_radius);
        contentLayout.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), cornerRadius);
            }
        });
        contentLayout.setClipToOutline(true);
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
        final float menuBottomMargin = getResources().getDimension(R.dimen.menu_bottom_margin);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomsheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {

            private float translationY = Integer.MIN_VALUE;
            private int collapseHeight = -1;
            private final float maxTranslationY = menuBottomMargin + cornerRadius;

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

                    if (Math.abs(translationY) <= maxTranslationY) {
                        v.setTranslationY(translationY);
                    } else if (translationY > maxTranslationY && v.getTranslationY() < maxTranslationY) {
                        // In case of fast changing
                        v.setTranslationY(maxTranslationY);
                    }
                }
            }
        });
        v.findViewById(R.id.container).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissAllowingStateLoss();
            }
        });
        bookmarksIcon = v.findViewById(R.id.img_bookmarks);
        bookmarksSelectedIcon = v.findViewById(R.id.img_bookmarks_selected);
        v.findViewById(R.id.bookmarks).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showItem(TYPE_BOOKMARKS);
                TelemetryWrapper.showPanelBookmark();
            }
        });
        downloadsIcon = v.findViewById(R.id.img_downloads);
        downloadsSelectedIcon = v.findViewById(R.id.img_downloads_selected);
        v.findViewById(R.id.downloads).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showItem(TYPE_DOWNLOADS);
                TelemetryWrapper.showPanelDownload();
            }
        });
        historyIcon = v.findViewById(R.id.img_history);
        historySelectedIcon = v.findViewById(R.id.img_history_selected);
        v.findViewById(R.id.history).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showItem(TYPE_HISTORY);
                TelemetryWrapper.showPanelHistory();
            }
        });
        screenshotsIcon = v.findViewById(R.id.img_screenshots);
        screenshotsSelectedIcon = v.findViewById(R.id.img_screenshots_selected);
        v.findViewById(R.id.screenshots).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showItem(TYPE_SCREENSHOTS);
                TelemetryWrapper.showPanelCapture();
            }
        });
        return v;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (this.onDismissListener != null) {
            this.onDismissListener.onDismiss(dialog);
        }
        super.onDismiss(dialog);
    }

    public void setOnDismissListener(DialogInterface.OnDismissListener listener) {
        this.onDismissListener = listener;
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
        }
    }

    private void showPanelFragment(PanelFragment panelFragment) {
        getChildFragmentManager().beginTransaction().replace(R.id.main_content, panelFragment).commit();
    }

    private void toggleSelectedItem() {
        firstLaunch = false;

        bookmarksIcon.setSelected(false);
        downloadsIcon.setSelected(false);
        historyIcon.setSelected(false);
        screenshotsIcon.setSelected(false);
        bookmarksSelectedIcon.setVisibility(View.INVISIBLE);
        downloadsSelectedIcon.setVisibility(View.INVISIBLE);
        historySelectedIcon.setVisibility(View.INVISIBLE);
        screenshotsSelectedIcon.setVisibility(View.INVISIBLE);

        switch (getArguments().getInt(TYPE)) {
            case TYPE_BOOKMARKS:
                bookmarksIcon.setSelected(true);
                bookmarksSelectedIcon.setVisibility(View.VISIBLE);
                break;
            case TYPE_DOWNLOADS:
                downloadsIcon.setSelected(true);
                downloadsSelectedIcon.setVisibility(View.VISIBLE);
                break;
            case TYPE_HISTORY:
                historyIcon.setSelected(true);
                historySelectedIcon.setVisibility(View.VISIBLE);
                break;
            case TYPE_SCREENSHOTS:
                screenshotsIcon.setSelected(true);
                screenshotsSelectedIcon.setVisibility(View.VISIBLE);
                break;
            default:
                throw new RuntimeException("There is no view type " + getArguments().getInt(TYPE));
        }
    }
}
