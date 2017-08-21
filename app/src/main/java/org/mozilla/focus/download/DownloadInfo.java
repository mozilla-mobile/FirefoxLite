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

    private Long DownloadId;
    private int Status;
    private String Size;
    private String Date;
    private String FileName;
    private String MediaUri;
    private String MimeType;
    private String FileUri;

    public DownloadInfo(){
    }
    public void setStatusInt(int status){
        Status = status;
    }

    public int getStatus(){
        return Status;
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

}
