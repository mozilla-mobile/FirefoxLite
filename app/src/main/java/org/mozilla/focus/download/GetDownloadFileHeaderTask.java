package org.mozilla.focus.download;

import android.net.TrafficStats;
import android.os.AsyncTask;
import android.util.Log;

import org.mozilla.focus.network.SocketTags;
import org.mozilla.focus.utils.AppConstants;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.SSLHandshakeException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


public class GetDownloadFileHeaderTask extends AsyncTask<String, Void, GetDownloadFileHeaderTask.HeaderInfo> {

    private static String TAG = "GetDownloadFileHeaderTask";

    public static class HeaderInfo {
        @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
        public boolean isSupportRange = false;
        @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
        public boolean isValidSSL = true;
        @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
        public long contentLength = 0L;
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
            String strContentLength = connection.getHeaderField("Content-Length");
            if (headerField != null && headerField.equals("bytes")) {
                headerInfo.isSupportRange = true;
            }
            if (strContentLength != null) {
                headerInfo.contentLength = Long.parseLong(strContentLength);
            }
            connection.getResponseCode();
            connection.disconnect();

        } catch (SSLHandshakeException e) {
            headerInfo.isValidSSL = false;
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "IOException");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Exception");
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return headerInfo;
    }
}
