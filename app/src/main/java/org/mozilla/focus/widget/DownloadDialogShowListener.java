package org.mozilla.focus.widget;

import android.content.DialogInterface;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import org.mozilla.focus.R;

/**
 * Created by anlin on 01/08/2017.
 */

public class DownloadDialogShowListener implements DialogInterface.OnShowListener
        ,DialogInterface.OnCancelListener ,DialogInterface.OnDismissListener{

    private View mMainView;

    public DownloadDialogShowListener(View view){
        mMainView = view;
    }

    @Override
    public void onShow(DialogInterface dialogInterface) {
        RecyclerView downloadList = (RecyclerView) mMainView.findViewById(R.id.list);
        downloadList.setAdapter(new DownloadListAdapter());
        downloadList.setLayoutManager(new LinearLayoutManager(mMainView.getContext(),LinearLayoutManager.VERTICAL,false));
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
    }
}
