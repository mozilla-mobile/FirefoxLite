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
import org.mozilla.focus.utils.DownloadReceiver;

/**
 * Created by anlin on 01/08/2017.
 */

public class DownloadDialogShowListener implements DialogInterface.OnShowListener
        ,DialogInterface.OnCancelListener ,DialogInterface.OnDismissListener{

    private View mMainView;
    private DownloadListAdapter mDownloadListAdapter;
    private BroadcastReceiver mDownloadReceiver;

    public DownloadDialogShowListener(View view){
        mMainView = view;
        RecyclerView downloadList = (RecyclerView) mMainView.findViewById(R.id.list);
        mDownloadListAdapter = new DownloadListAdapter();
        downloadList.setAdapter(mDownloadListAdapter);
        downloadList.setLayoutManager(new LinearLayoutManager(mMainView.getContext(),LinearLayoutManager.VERTICAL,true));
    }

    @Override
    public void onShow(DialogInterface dialogInterface) {
        mDownloadListAdapter.fetchEntity();

        IntentFilter intentFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        mDownloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L);
                mDownloadListAdapter.updateItem(downloadId);
            }
        };

        mMainView.getContext().registerReceiver(mDownloadReceiver,intentFilter);
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        mMainView.getContext().unregisterReceiver(mDownloadReceiver);
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
    }
}
