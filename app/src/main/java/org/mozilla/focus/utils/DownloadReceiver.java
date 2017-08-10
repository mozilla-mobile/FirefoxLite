package org.mozilla.focus.utils;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.query.QueryBuilder;
import org.mozilla.focus.greenDAO.DBUtils;
import org.mozilla.focus.greenDAO.DownloadInfo;
import org.mozilla.focus.greenDAO.DownloadInfoEntity;
import org.mozilla.focus.greenDAO.DownloadInfoEntityDao;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anlin on 31/07/2017.
 */

public class DownloadReceiver extends BroadcastReceiver {

    private static DownloadReceiver mDownloadReceiver;
    private List<OnCompleteListener> listeners = new ArrayList<>();

    public interface OnCompleteListener{
        void onCompleted(DownloadInfo downloadInfo);
    }

    public static DownloadReceiver getDownloadReceiver(){
        if (mDownloadReceiver == null){
            mDownloadReceiver = new DownloadReceiver();
        }
        return mDownloadReceiver;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())){
            long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L);
            DownloadInfo downloadInfo = DBUtils.getDbService().getDownloadInfo(downloadId);

            for (int i=0;i<listeners.size();i++){
                listeners.get(i).onCompleted(downloadInfo);
            }
        }
    }

    //Temporary
    public void setOnCompleteListener(OnCompleteListener listener){
        listeners.add(listener);
    }
}
