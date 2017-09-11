package org.mozilla.focus.fragment;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.mozilla.focus.R;
import org.mozilla.focus.download.DownloadInfo;
import org.mozilla.focus.download.DownloadInfoManager;
import org.mozilla.focus.widget.DownloadListAdapter;

import java.util.List;

public class DownloadsFragment extends PanelFragment {

    private RecyclerView recyclerView;
    private DownloadListAdapter mDownloadListAdapter;
    private BroadcastReceiver mDownloadReceiver;

    public static DownloadsFragment newInstance() {
        return new DownloadsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        recyclerView = (RecyclerView) inflater.inflate(R.layout.fragment_downloads, container, false);
        return recyclerView;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDownloadListAdapter = new DownloadListAdapter(getContext());
        mDownloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L);
                if (id>0){
                    DownloadInfoManager.getInstance().queryCertainId(id, new DownloadInfoManager.AsyncQueryListener() {
                        @Override
                        public void onQueryComplete(List downloadInfoList) {
                            for (int i=0;i<downloadInfoList.size();i++){
                                DownloadInfo downloadInfo = (DownloadInfo) downloadInfoList.get(i);
                                mDownloadListAdapter.updateItem(downloadInfo);
                            }
                        }
                    });
                }
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        getActivity().registerReceiver(mDownloadReceiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mDownloadReceiver);
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prepare();
    }

    private void prepare() {
        recyclerView.setAdapter(mDownloadListAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(),LinearLayoutManager.VERTICAL,true));
    }
}
