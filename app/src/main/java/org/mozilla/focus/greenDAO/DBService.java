package org.mozilla.focus.greenDAO;

import android.app.DownloadManager;
import android.content.Context;

import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.query.QueryBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anlin on 27/07/2017.
 */

public class DBService {
    private static final String DB_NAME = "Zerda.db";
    private DaoSession daoSession;
    private DownloadManager downloadManager;

    public void init(Context context){
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(context, DB_NAME);

        DaoMaster daoMaster = new DaoMaster(helper.getWritableDatabase());

        daoSession = daoMaster.newSession();

        downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
    }

    public DownloadInfoEntityDao getDao(){
        return daoSession.getDownloadInfoEntityDao();
    }

    public DownloadManager getDownloadManager(){
        return downloadManager;
    }

    public long insert(Long downloadId,String fileName){
        DownloadInfoEntity entity = new DownloadInfoEntity(null,downloadId,fileName);
        return getDao().insert(entity);
    }

    public void delete(long downloadId){
        QueryBuilder<DownloadInfoEntity> queryBuilder = getDao().queryBuilder();
        Property downloadIdProperty = DownloadInfoEntityDao.Properties.DownLoadId;
        DownloadInfoEntity entity = queryBuilder.where(downloadIdProperty.eq(downloadId)).unique();
        getDao().delete(entity);
    }

    public List<DownloadInfo> getAllDownloadInfo(){
        List<DownloadInfoEntity> downloadIdList = getDao().loadAll();
        List<DownloadInfo> downloadInfoList = new ArrayList<>();

        for (int i = 0 ;i<downloadIdList.size();i++){
            DownloadInfoEntity entity = downloadIdList.get(i);
            DownloadInfo downloadInfo = new DownloadInfo(entity.getDownLoadId(),entity.getFileName());
            downloadInfoList.add(downloadInfo);
        }

        return downloadInfoList;
    }

    public DownloadInfo getDownloadInfo(long downloadId){
        QueryBuilder<DownloadInfoEntity> queryBuilder = getDao().queryBuilder();
        Property downloadIdProperty = DownloadInfoEntityDao.Properties.DownLoadId;
        DownloadInfoEntity entity = queryBuilder.where(downloadIdProperty.eq(downloadId)).unique();
        return new DownloadInfo(entity.getDownLoadId(),entity.getFileName());
    }
}
