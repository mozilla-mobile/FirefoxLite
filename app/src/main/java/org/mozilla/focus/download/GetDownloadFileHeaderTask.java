package org.mozilla.focus.download;

import android.net.TrafficStats;
import android.os.AsyncTask;

import org.mozilla.focus.network.SocketTags;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.SSLHandshakeException;


public class GetDownloadFileHeaderTask extends AsyncTask<String, Void, GetDownloadFileHeaderTask.HeaderInfo> {

    public static class HeaderInfo {
        boolean isSupportRange;
        boolean isSupportSSL;
    }

    @Override
    protected HeaderInfo doInBackground(String... params) {
        TrafficStats.setThreadStatsTag(SocketTags.DOWNLOADS);
        HttpURLConnection connection = null;
        boolean supportRange = false;
        boolean isSSL = true;
        int responseCode = 0;
        try {
            connection = (HttpURLConnection) new URL(params[0]).openConnection();
            connection.setRequestMethod("HEAD");
            String headerField = connection.getHeaderField("Accept-Ranges");
            if (headerField != null && headerField.equals("bytes")) {
                supportRange = true;
            }
            responseCode = connection.getResponseCode();
            connection.disconnect();

        } catch (SSLHandshakeException e) {
            isSSL = false;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        if (responseCode == HttpURLConnection.HTTP_OK) {
            HeaderInfo headerInfo = new HeaderInfo();
            headerInfo.isSupportRange = supportRange;
            headerInfo.isSupportSSL = isSSL;
            return headerInfo;
        } else {
            return null;
        }
    }
}
