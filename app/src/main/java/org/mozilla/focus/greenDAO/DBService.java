package org.mozilla.focus.greenDAO;

import android.app.DownloadManager;
import android.content.Context;

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
}
