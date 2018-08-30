package org.mozilla.httptask;

import android.net.TrafficStats;
import android.os.AsyncTask;

import org.mozilla.httprequest.HttpRequest;

import java.net.MalformedURLException;
import java.net.URL;

public class SimpleLoadUrlTask extends AsyncTask<String, Void, String> {

    /**
     *
     * @param strings
     * strings[0] is url
     * strings[1] is userAgent
     * strings[2] is SocketTag(intString)
     * @return
     */
    @Override
    protected String doInBackground(String... strings) {
        String line;
        try {
            // TODO: 8/6/18 Check range
            TrafficStats.setThreadStatsTag(Integer.parseInt(strings[2]));
        } catch (NumberFormatException ignored) {
            throw new IllegalArgumentException("Socket Tag should be a number");
        }
        try {
            line = HttpRequest.get(new URL(strings[0]), strings[1]);
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException("MalformedURLException");
        }
        return line;
    }
}
