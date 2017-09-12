package org.mozilla.focus.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import static org.mozilla.focus.provider.DownloadContract.Download;

/**
 * Created by anlin on 17/08/2017.
 */

public class DownloadInfoDbHelper {
    // Database Info
    private static final String DATABASE_NAME = "DownloadInfo.db";
    private static final int DATABASE_VERSION = 2;

    private static final String DROP_TABLE_IF_EXISTS = "DROP TABLE IF EXISTS ";
    private static final String CREATE_TABLE_IF_NOT_EXISTS = "CREATE TABLE ";

    private static DownloadInfoDbHelper sInstance;
    private final OpenHelper mOpenHelper;


    private static final class OpenHelper extends SQLiteOpenHelper{

        public OpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {

            String CREATE_TABLE =CREATE_TABLE_IF_NOT_EXISTS + Download.TABLE_DOWNLOAD + "("
                    + Download._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + Download.DOWNLOAD_ID+ " INTEGER,"
                    + Download.FILE_PATH + " TEXT"
                    + ")";

            sqLiteDatabase.execSQL(CREATE_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase,int oldVersion,int newVersion) {
            if (oldVersion != newVersion){
                sqLiteDatabase.execSQL(DROP_TABLE_IF_EXISTS + Download.TABLE_DOWNLOAD);
                onCreate(sqLiteDatabase);
            }

        }
    }

    private DownloadInfoDbHelper(Context context){

        mOpenHelper = new OpenHelper(context,DATABASE_NAME,null,DATABASE_VERSION);
    }

    public static synchronized DownloadInfoDbHelper getsInstance(Context context){
        if (sInstance == null){
            sInstance = new DownloadInfoDbHelper(context);
        }

        return sInstance;
    }

    public SQLiteDatabase getReadableDB(){
        return mOpenHelper.getReadableDatabase();
    }

    public SQLiteDatabase getWritableDB(){
        return mOpenHelper.getWritableDatabase();
    }
}
