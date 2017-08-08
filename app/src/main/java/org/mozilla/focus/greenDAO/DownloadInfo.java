package org.mozilla.focus.greenDAO;

import android.app.DownloadManager;
import android.database.Cursor;
import android.webkit.MimeTypeMap;

import java.util.Calendar;
import java.util.Formatter;

/**
 * Created by anlin on 27/07/2017.
 */

public class DownloadInfo {

    private Long DownloadId;
    private String Status;
    private String Size;
    private String Date;
    private String FileName;
    private String Uri;
    private String MimeType;

    public DownloadInfo(long downloadId,String fileName){

        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        Cursor cursor = DBUtils.getDbService().getDownloadManager().query(query);

        if (cursor.moveToFirst()){
            int status =cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            double size = cursor.getDouble(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
            long timeStamp = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP));

            Uri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
            String extension = MimeTypeMap.getFileExtensionFromUrl(Uri);
            MimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            cursor.close();

            Status = statusConvertStr(status);
            Size = convertByteToReadable(size);
            Date = convertMillis(timeStamp);
        }

        DownloadId = downloadId;
        FileName = fileName;
    }

    public String getMimeType(){
        return MimeType;
    }

    public String getUri(){
        return Uri;
    }
    public Long getDownloadId(){
        return DownloadId;
    }

    public String getFileName(){
        return FileName;
    }

    public String getStatus(){
        return Status;
    }

    public String getSize(){
        return Size;
    }

    public String getDate(){
        return Date;
    }

    private String convertMillis(long millis){
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        return new Formatter().format("%tB %td",calendar,calendar).toString();
    }

    private String convertByteToReadable(double bytes){
        String[] dictionary = { "bytes", "KB", "MB", "GB"};

        int index;
        for (index = 0; index < dictionary.length;index++){
            if (bytes < 1024) {
                break;
            }
            bytes = bytes / 1024;
        }
        return String.format("%.1f",bytes)+dictionary[index];
    }

    private String statusConvertStr(int status){
        switch(status) {
            case DownloadManager.STATUS_PAUSED:
                return "paused";
            case DownloadManager.STATUS_PENDING:
                return "pending";
            case DownloadManager.STATUS_RUNNING:
                return "running";
            case DownloadManager.STATUS_SUCCESSFUL:
                return "successful";
            case DownloadManager.STATUS_FAILED:
                return "failed";
            default:
                return "";
        }

    }
}
