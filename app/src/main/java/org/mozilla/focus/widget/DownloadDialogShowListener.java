package org.mozilla.focus.widget;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import org.mozilla.focus.R;
import org.mozilla.focus.greenDAO.DownloadInfo;
import org.mozilla.focus.utils.DownloadReceiver;

/**
 * Created by anlin on 01/08/2017.
 */

public class DownloadDialogShowListener implements DialogInterface.OnShowListener
        ,DialogInterface.OnCancelListener ,DialogInterface.OnDismissListener
        ,DownloadReceiver.OnCompleteListener{

    private DownloadListAdapter mDownloadListAdapter;

    public DownloadDialogShowListener(View view){
        RecyclerView downloadList = (RecyclerView) view.findViewById(R.id.list);
        mDownloadListAdapter = new DownloadListAdapter();
        downloadList.setAdapter(mDownloadListAdapter);
        downloadList.setLayoutManager(new LinearLayoutManager(view.getContext(),LinearLayoutManager.VERTICAL,true));

        DownloadReceiver.getDownloadReceiver().setOnCompleteListener(this);
    }

    @Override
    public void onShow(DialogInterface dialogInterface) {
        mDownloadListAdapter.fetchEntity();
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {

    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
    }

    @Override
    public void onCompleted(DownloadInfo downloadInfo) {
        if (mDownloadListAdapter != null){
            mDownloadListAdapter.updateItem(downloadInfo);
        }
    }
}
