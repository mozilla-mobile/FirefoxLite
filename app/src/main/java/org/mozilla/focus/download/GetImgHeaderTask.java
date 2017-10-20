package org.mozilla.focus.download;

import android.os.AsyncTask;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by anlin on 20/10/2017.
 */

public class GetImgHeaderTask extends AsyncTask<String, Void, String> {

    public interface Callback {
        void setMIMEType(String mimeType);
    }

    public GetImgHeaderTask.Callback callback;

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @Override
    protected String doInBackground(String... params) {
        HttpURLConnection connection = null;
        String contentType = "";
        int responseCode = 0;
        try {

            connection = (HttpURLConnection) new URL(params[0]).openConnection();
            connection.setRequestMethod("HEAD");
            contentType = connection.getContentType();
            responseCode = connection.getResponseCode();

            connection.disconnect();

        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        if (responseCode == HttpURLConnection.HTTP_OK) {
            return contentType;
        } else {
            return null;
        }
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);

        this.callback.setMIMEType(s);
    }
}
