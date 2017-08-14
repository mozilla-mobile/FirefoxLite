package org.mozilla.focus.fragment;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.DialogFragment;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

import org.mozilla.focus.R;
import org.mozilla.focus.widget.DownloadListAdapter;

public class ListPanelDialog extends DialogFragment {

    public final static int TYPE_DOWNLOAD = 1;
    public final static int TYPE_HISTORY = 2;
    public final static int TYPE_SCREENSHOTS = 3;

    private NestedScrollView scrollView;
    private static final String TYPE = "TYPE";

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
                throw new RuntimeException("There is not view type " + getArguments().getInt(TYPE));
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
        v.findViewById(R.id.download).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDownload();
            }
        });
        v.findViewById(R.id.history).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHistory();
            }
        });
        v.findViewById(R.id.screenshots).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showScreenshots();
            }
        });
        return v;
    }

    private void showDownload() {
        DownloadFragment downloadFragment = DownloadFragment.newInstance();
        getChildFragmentManager().beginTransaction().replace(R.id.main_content, downloadFragment).commit();
    }

    private void showHistory() {

    }

    private void showScreenshots() {

    }
}
