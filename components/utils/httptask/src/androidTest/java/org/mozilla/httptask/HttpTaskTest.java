package org.mozilla.httptask;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

@RunWith(AndroidJUnit4.class)
public class HttpTaskTest {

    private static final String PATH = "/path";
    private static final String RESPONSE_BODY = "body";
    private static final int SOCKET_TAG = 1234;

    @Test
    public void fetchSimpleAPI() {
        MockWebServer webServer = new MockWebServer();
        try {
            webServer.enqueue(new MockResponse()
                    .setBody(RESPONSE_BODY)
                    .addHeader("Set-Cookie", "sphere=battery; Expires=Wed, 21 Oct 2035 07:28:00 GMT;"));
            webServer.start();
        } catch (IOException e) {
            throw new AssertionError("Could not start web server", e);
        }
        final String targetUrl = webServer.url(PATH).toString();
        new SimpleLoadUrlTask() {

            @Override
            protected void onPostExecute(String line) {
                Assert.assertEquals(line, RESPONSE_BODY);
            }


        }.execute(targetUrl, null, Integer.toString(SOCKET_TAG));
    }
}
