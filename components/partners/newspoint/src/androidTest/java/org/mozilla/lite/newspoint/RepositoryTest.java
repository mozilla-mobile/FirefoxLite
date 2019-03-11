package org.mozilla.lite.newspoint;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.CountDownLatch;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.mozilla.lite.partner.Repository;

@RunWith(AndroidJUnit4.class)
public class RepositoryTest {

    private static final int SOCKET_TAG = 1234;
    private static final String FAKE_PATH = "/atp?channel=*&section=top-news&lang=english&curpg=%d&pp=%d&v=v1";

    @Test
    public void testParsing() throws InterruptedException {
        try {
            final CountDownLatch countDownLatch = new CountDownLatch(1);
            final int LOAD_SIZE = 30;
            final MockWebServer webServer = new MockWebServer();
            final InputStream inputStream = InstrumentationRegistry.getContext().getResources().openRawResource(org.mozilla.lite.newspoint.test.R.raw.response);
            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            final String response = bufferedReader.readLine();
            webServer.enqueue(new MockResponse()
                    .setBody(response)
                    .addHeader("Set-Cookie", "sphere=battery; Expires=Wed, 21 Oct 2035 07:28:00 GMT;"));
            webServer.start();
            Repository repository = new Repository<NewsPointItem>(InstrumentationRegistry.getContext(), null, SOCKET_TAG, itemPojoList -> {
                Assert.assertEquals(LOAD_SIZE, itemPojoList.size());
                countDownLatch.countDown();
            }, null, "FAKE", RepositoryNewsPoint.FIRST_PAGE, RepositoryNewsPoint.PARSER) {
                @Override
                protected String getSubscriptionUrl(int pageNumber) {
                    return webServer.url(FAKE_PATH).toString();
                }
            };
            countDownLatch.await();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
