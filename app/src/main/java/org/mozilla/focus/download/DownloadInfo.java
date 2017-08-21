package org.mozilla.focus.download;

import android.app.DownloadManager;
import android.content.Context;

import org.mozilla.focus.R;

import java.util.Calendar;
import java.util.Formatter;

/**
 * Created by anlin on 27/07/2017.
 */

public class DownloadInfo {

    private Context mContext;
    private Long DownloadId;
    private String StatusStr;
    private int StatusInt;
    private String Size;
    private String Date;
    private String FileName;
    private String MediaUri;
    private String MimeType;
    private String FileUri;

    public DownloadInfo(Context context){
        mContext = context;
    }
    public void setStatusInt(int statusInt){
        StatusInt = statusInt;
    }

    public int getStatusInt(){
        return StatusInt;
    }
    public void setFileUri(String fileUri){
        FileUri = fileUri;
    }
    public String getFileUri(){
        return FileUri;
    }

    public void setMimeType(String mimeType){
        MimeType = mimeType;
    }

    public String getMimeType(){
        return MimeType;
    }

    public void setMediaUri(String mediaUri){
        MediaUri = mediaUri;
    }

    public String getMediaUri(){
        return MediaUri;
    }

    public void setDownloadId(Long downloadId){
        DownloadId = downloadId;
    }
    public Long getDownloadId(){
        return DownloadId;
    }

    public void setFileName(String fileName){
        FileName = fileName;
    }

    public String getFileName(){
        return FileName;
    }

    public void setStatusStr(int status){
        StatusStr = statusConvertStr(status);
    }

    public String getStatusStr(){
        return StatusStr;
    }

    public void setSize(double size){
        Size = convertByteToReadable(size);
    }

    public String getSize(){
        return Size;
    }

    public void setDate(long millis){
        Date = convertMillis(millis);
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
                return mContext.getResources().getString(R.string.pause);
            case DownloadManager.STATUS_PENDING:
                return mContext.getResources().getString(R.string.pending);
            case DownloadManager.STATUS_RUNNING:
                return mContext.getResources().getString(R.string.running);
            case DownloadManager.STATUS_SUCCESSFUL:
                return mContext.getResources().getString(R.string.successful);
            case DownloadManager.STATUS_FAILED:
                return mContext.getResources().getString(R.string.failed);
            default:
                return mContext.getResources().getString(R.string.unknown);
        }

    }
}
