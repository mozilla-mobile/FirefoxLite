package org.mozilla.rocket.bhaskar;

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
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

@RunWith(AndroidJUnit4.class)
public class RepositoryTest {

    private static final int SOCKET_TAG = 1234;
    private static final String FAKE_PATH = "/firefox?pageSize=%d&channel_slno=%d&pageNumber=%d";

    @Test
    public void testParsing() throws InterruptedException {
        try {
            final CountDownLatch countDownLatch = new CountDownLatch(1);
            final int LOAD_SIZE = 30;
            final MockWebServer webServer = new MockWebServer();
            final InputStream inputStream = InstrumentationRegistry.getContext().getResources().openRawResource(org.mozilla.rocket.bhaskar.test.R.raw.response);
            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            final String response = bufferedReader.readLine();
            webServer.enqueue(new MockResponse()
                    .setBody(response)
                    .addHeader("Set-Cookie", "sphere=battery; Expires=Wed, 21 Oct 2035 07:28:00 GMT;"));
            webServer.start();
            Repository repository = new Repository(InstrumentationRegistry.getContext(), 521, LOAD_SIZE, null, SOCKET_TAG, itemPojoList -> {
                Assert.assertEquals(LOAD_SIZE, itemPojoList.size());
                countDownLatch.countDown();
            }, webServer.url(FAKE_PATH).toString());
            countDownLatch.await();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testLoadingAndLoadMore() throws InterruptedException {
        final CountDownLatch countDownLatch1 = new CountDownLatch(1);
        final CountDownLatch countDownLatch2 = new CountDownLatch(1);
        final int LOAD_SIZE = 3;
        final AtomicInteger atomicInteger = new AtomicInteger(0);
        Repository repository = new Repository(InstrumentationRegistry.getContext(), 521, LOAD_SIZE, null, SOCKET_TAG, itemPojoList -> {
            if (atomicInteger.intValue() == 0) {
                Assert.assertEquals(LOAD_SIZE, itemPojoList.size());
                countDownLatch1.countDown();
            }
            if (atomicInteger.intValue() == 1) {
                Assert.assertEquals(LOAD_SIZE * 2, itemPojoList.size());
                countDownLatch2.countDown();
            }
            atomicInteger.incrementAndGet();
        });
        countDownLatch1.await();
        repository.loadMore();
        countDownLatch2.await();
    }
}
