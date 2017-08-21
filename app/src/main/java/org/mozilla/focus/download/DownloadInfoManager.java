package org.mozilla.focus.download;

import android.app.DownloadManager;
import android.content.AsyncQueryHandler;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.webkit.MimeTypeMap;
import java.util.ArrayList;
import java.util.List;
import static org.mozilla.focus.provider.DownloadContract.Download;

/**
 * Created by anlin on 17/08/2017.
 */

public class DownloadInfoManager {

    private static final int TOKEN = 2;
    private static DownloadInfoManager sInstance;
    private static Context mContext;
    private static DownloadInfoQueryHandler mQueryHandler;

    public static DownloadInfoManager getInstance(){
        if (sInstance == null){
            sInstance = new DownloadInfoManager();
        }
        return  sInstance;
    }

    private static final class DownloadInfoQueryHandler extends AsyncQueryHandler{

        public DownloadInfoQueryHandler(Context context) {
            super(context.getContentResolver());
        }

        @Override
        protected void onInsertComplete(int token, Object cookie, Uri uri) {
            switch (token){
                case TOKEN:
                    if (cookie != null){
                        final long id = uri == null ? -1 : Long.parseLong(uri.getLastPathSegment());
                        ((AsyncInsertListener) cookie).onInsertComplete(id);
                    }
                    break;
                default:
                    break;
            }
        }

        @Override
        protected void onDeleteComplete(int token, Object cookie, int result) {
            switch (token){
                case TOKEN:
                    if (cookie != null){
                        AsyncDeleteWrapper wrapper = ((AsyncDeleteWrapper) cookie);
                        if (wrapper.listener != null) {
                            wrapper.listener.onDeleteComplete(result, wrapper.id);
                        }
                    }
                    break;
                default:
                    break;
            }
        }

        @Override
        protected void onUpdateComplete(int token, Object cookie, int result) {
            switch (token){
                case TOKEN:
                    if (cookie != null){
                        ((AsyncUpdateListener)cookie).onUpdateComplete(result);
                    }
                    break;
                default:
                    break;
            }
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            switch (token){
                case TOKEN:
                    if (cookie != null){
                        List<DownloadInfo> downloadInfoList = new ArrayList<>();
                        if (cursor != null){
                            while (cursor.moveToNext()){
                                downloadInfoList.add(cursorToDownloadInfo(cursor));
                            }
                            cursor.close();
                        }

                        ((AsyncQueryListener)cookie).onQueryComplete(downloadInfoList);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private static final class AsyncDeleteWrapper {

        public long id;
        public AsyncDeleteListener listener;

        public AsyncDeleteWrapper(long id, AsyncDeleteListener listener) {
            this.id = id;
            this.listener = listener;
        }
    }

    public interface ContentChangeListener {
        void onContentChanged();
    }

    public interface AsyncInsertListener {
        void onInsertComplete(long id);
    }

    public interface AsyncDeleteListener {
        void onDeleteComplete(int result, long id);
    }

    public interface AsyncUpdateListener {
        void onUpdateComplete(int result);
    }

    public interface AsyncQueryListener {
        void onQueryComplete(List downloadInfoList);
    }


    public static void init(Context context){
        mContext = context;
        mQueryHandler =  new DownloadInfoQueryHandler(context);
    }

    public void insert(DownloadInfo downloadInfo,AsyncInsertListener listener){
        mQueryHandler.startInsert(TOKEN,listener
                , Download.CONTENT_URI,getContentValuesFromDownloadInfo(downloadInfo));
    }

    public void delete(Long downloadId,AsyncDeleteListener listener){
        mQueryHandler.startDelete(TOKEN,new AsyncDeleteWrapper(downloadId,listener)
                ,Download.CONTENT_URI,Download.DOWNLOAD_ID + " = ?", new String[] {Long.toString(downloadId)});
    }

    public void update(DownloadInfo downloadInfo,AsyncUpdateListener listener){
        mQueryHandler.startUpdate(TOKEN,listener,Download.CONTENT_URI,getContentValuesFromDownloadInfo(downloadInfo)
                ,Download.DOWNLOAD_ID + " = ?", new String[] {Long.toString(downloadInfo.getDownloadId())});
    }

    public void query(int offset,int limit,AsyncQueryListener listener){
        mQueryHandler.startQuery(TOKEN,listener,Uri.parse(Download.CONTENT_URI.toString()+"?offset=" + offset + "&limit=" + limit),null,null,null,null);
    }

    private static ContentValues getContentValuesFromDownloadInfo(DownloadInfo downloadInfo){
        ContentValues contentValues = new ContentValues();

        contentValues.put(Download.DOWNLOAD_ID,downloadInfo.getDownloadId());
        contentValues.put(Download.FILE_NAME,downloadInfo.getFileName());

        return contentValues;
    }
    private static DownloadInfo cursorToDownloadInfo(Cursor cursor){
        DownloadInfo downloadInfo = new DownloadInfo(mContext);
        downloadInfo.setDownloadId(cursor.getLong(cursor.getColumnIndex(Download.DOWNLOAD_ID)));
        downloadInfo.setFileName(cursor.getString(cursor.getColumnIndex(Download.FILE_NAME)));

        //query download manager
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadInfo.getDownloadId());
        DownloadManager manager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
        Cursor managerCursor = manager.query(query);

        try {
            if (managerCursor.moveToFirst()){
                int status = managerCursor.getInt(managerCursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                downloadInfo.setStatusStr(status);
                downloadInfo.setStatusInt(status);

                double size = managerCursor.getDouble(managerCursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                downloadInfo.setSize(size);

                long timeStamp = managerCursor.getLong(managerCursor.getColumnIndex(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP));
                downloadInfo.setDate(timeStamp);

                downloadInfo.setMediaUri(managerCursor.getString(managerCursor.getColumnIndex(DownloadManager.COLUMN_MEDIAPROVIDER_URI)));
                downloadInfo.setFileUri(managerCursor.getString(managerCursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)));

                String extension = MimeTypeMap.getFileExtensionFromUrl(downloadInfo.getFileUri());
                downloadInfo.setMimeType(MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension));
            }

        }catch (Exception e){
            managerCursor.close();
        }finally {
            if (managerCursor != null){
                managerCursor.close();
            }
        }

        return downloadInfo;
    }

}
