package org.mozilla.focus.download;

import android.net.TrafficStats;
import android.os.AsyncTask;

import org.mozilla.focus.network.SocketTags;
import org.mozilla.focus.utils.AppConstants;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.SSLHandshakeException;


public class GetDownloadFileHeaderTask extends AsyncTask<String, Void, GetDownloadFileHeaderTask.HeaderInfo> {

    public static class HeaderInfo {
        boolean isSupportRange = false;
        boolean isValidSSL = true;
    }

    @Override
    protected HeaderInfo doInBackground(String... params) {
        TrafficStats.setThreadStatsTag(SocketTags.DOWNLOADS);
        HttpURLConnection connection = null;
        HeaderInfo headerInfo = new HeaderInfo();
        if (AppConstants.isDevBuild()) {
            return headerInfo;
        }
        try {
            connection = (HttpURLConnection) new URL(params[0]).openConnection();
            connection.setRequestMethod("HEAD");
            String headerField = connection.getHeaderField("Accept-Ranges");
            if (headerField != null && headerField.equals("bytes")) {
                headerInfo.isSupportRange = true;
            }
            connection.getResponseCode();
            connection.disconnect();

        } catch (SSLHandshakeException e) {
            headerInfo.isValidSSL = false;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return headerInfo;
    }
}
