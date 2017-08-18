package org.mozilla.focus.download;

import android.app.DownloadManager;

import java.util.Calendar;
import java.util.Formatter;

/**
 * Created by anlin on 27/07/2017.
 */

public class DownloadInfo {

    public static final String STATUS_PAUSED = "PAUSE";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_SUCCESSFUL = "SUCCESSFUL";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_UNKNOWN ="UNKNOWN";

    private Long DownloadId;
    private String Status;
    private String Size;
    private String Date;
    private String FileName;
    private String MediaUri;
    private String MimeType;
    private String FileUri;

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

    public void setStatus(int status){
        Status = statusConvertStr(status);
    }

    public String getStatus(){
        return Status;
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
                return STATUS_PAUSED;
            case DownloadManager.STATUS_PENDING:
                return STATUS_PENDING;
            case DownloadManager.STATUS_RUNNING:
                return STATUS_RUNNING;
            case DownloadManager.STATUS_SUCCESSFUL:
                return STATUS_SUCCESSFUL;
            case DownloadManager.STATUS_FAILED:
                return STATUS_FAILED;
            default:
                return STATUS_UNKNOWN;
        }

    }
}
