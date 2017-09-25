package org.mozilla.focus.download;

import android.text.TextUtils;
import java.util.Calendar;
import java.util.Formatter;

/**
 * Created by anlin on 27/07/2017.
 */

public class DownloadInfo {

    private Long ColumnId;
    private Long DownloadId;
    private int Status;
    private String Size ;
    private String Date ;
    private String FileName = "";
    private String MediaUri ="";
    private String MimeType = "";
    private String FileUri ="";
    private String FileExtension = "";

    public DownloadInfo(){}

    public void setColumnId(Long id){
        ColumnId = id;
    }

    public Long getColumnId(){
        return ColumnId;
    }

    public void setFileExtension(String fileExtension){
        if (!TextUtils.isEmpty(fileExtension)){
            FileExtension = fileExtension;
        }
    }

    public String getFileExtension(){
        return FileExtension;
    }
    public void setStatusInt(int status){
        Status = status;
    }

    public int getStatus(){
        return Status;
    }
    public void setFileUri(String fileUri){
        if (!TextUtils.isEmpty(fileUri))
        {
            FileUri = fileUri;
        }
    }
    public String getFileUri(){
        return FileUri;
    }

    public void setMimeType(String mimeType){
        if (!TextUtils.isEmpty(mimeType)){
            MimeType = mimeType;
        }
    }

    public String getMimeType(){
        return MimeType;
    }

    public void setMediaUri(String mediaUri){
        if (!TextUtils.isEmpty(mediaUri)){
            MediaUri = mediaUri;
        }
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

        if (!TextUtils.isEmpty(fileName)){
            FileName = fileName;
        }
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
