package org.mozilla.focus.fragment;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

import org.mozilla.focus.R;
import org.mozilla.focus.history.BrowsingHistoryFragment;
import org.mozilla.focus.widget.DownloadListAdapter;

public class ListPanelDialog extends DialogFragment {

    public final static int TYPE_DOWNLOAD = 1;
    public final static int TYPE_HISTORY = 2;
    public final static int TYPE_SCREENSHOTS = 3;

    private NestedScrollView scrollView;
    private static final String TYPE = "TYPE";
    private View downloadsTouchArea;
    private View historyTouchArea;
    private View screenshotsTouchArea;

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
            case TYPE_DOWNLOAD:
                showDownload();
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
    public void onStart() {
        super.onStart();
        // Hack to force full screen
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_listpanel_dialog, container, false);
        scrollView = (NestedScrollView) v.findViewById(R.id.main_content);
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
        downloadsTouchArea = v.findViewById(R.id.download);
        downloadsTouchArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDownload();
            }
        });
        historyTouchArea = v.findViewById(R.id.history);
        historyTouchArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHistory();
            }
        });
        screenshotsTouchArea = v.findViewById(R.id.screenshots);
        screenshotsTouchArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showScreenshots();
            }
        });
        return v;
    }

    private void setSelectedItem(int selectedItem) {
        getArguments().putInt(TYPE, selectedItem);
        toggleSelectedItem();
    }

    private void showDownload() {
        setSelectedItem(TYPE_DOWNLOAD);
        DownloadFragment downloadFragment = DownloadFragment.newInstance();
        getChildFragmentManager().beginTransaction().replace(R.id.main_content, downloadFragment).commit();
    }

    private void showHistory() {
        setSelectedItem(TYPE_HISTORY);
        BrowsingHistoryFragment browsingHistoryFragment = BrowsingHistoryFragment.newInstance();
        getChildFragmentManager().beginTransaction().replace(R.id.main_content, browsingHistoryFragment).commit();
    }

    private void showScreenshots() {
        setSelectedItem(TYPE_SCREENSHOTS);
        getChildFragmentManager().beginTransaction().replace(R.id.main_content, new Fragment()).commit();
    }

    private void toggleSelectedItem() {
        downloadsTouchArea.setSelected(false);
        historyTouchArea.setSelected(false);
        screenshotsTouchArea.setSelected(false);
        switch (getArguments().getInt(TYPE)) {
            case TYPE_DOWNLOAD:
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
