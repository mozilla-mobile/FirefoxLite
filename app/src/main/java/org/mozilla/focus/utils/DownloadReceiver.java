package org.mozilla.focus.utils;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.query.QueryBuilder;
import org.mozilla.focus.greenDAO.DBUtils;
import org.mozilla.focus.greenDAO.DownloadInfoEntity;
import org.mozilla.focus.greenDAO.DownloadInfoEntityDao;

/**
 * Created by anlin on 31/07/2017.
 */

public class DownloadReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L);
        //DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        //DownloadManager.Query query = new DownloadManager.Query();
        //query.setFilterById(downloadId);
        //Cursor cursor = downloadManager.query(query);

        QueryBuilder<DownloadInfoEntity> queryBuilder = DBUtils.getDbService().getDao().queryBuilder();
        Property downloadIdProperty = DownloadInfoEntityDao.Properties.DownLoadId;
        DownloadInfoEntity downloadInfoEntity = queryBuilder.where(downloadIdProperty.eq(downloadId)).unique();

        //should't be empty and in the local SQLite db.
        if (!downloadInfoEntity.getFileName().isEmpty()){
            //update the status to UI
        }
    }


}
