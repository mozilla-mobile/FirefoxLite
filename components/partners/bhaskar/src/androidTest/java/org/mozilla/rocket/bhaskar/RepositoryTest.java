package org.mozilla.rocket.bhaskar;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.mozilla.lite.partner.Repository;

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
            Repository repository = new Repository<BhaskarItem>(InstrumentationRegistry.getContext(), null, SOCKET_TAG, itemPojoList -> {
                Assert.assertEquals(LOAD_SIZE, itemPojoList.size());
                countDownLatch.countDown();
            }, null, "FAKE", RepositoryBhaskar.FIRST_PAGE, RepositoryBhaskar.PARSER) {
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

    // This task is flasky since it assumes cache is always faster
    @Test
    public void testLoadingAndLoadMore() throws InterruptedException {
        final CountDownLatch countDownLatch1 = new CountDownLatch(1);
        final CountDownLatch countDownLatch2 = new CountDownLatch(1);
        final int LOAD_SIZE = 3;
        final AtomicInteger atomicInteger = new AtomicInteger(0);
        final List<BhaskarItem> firstResult = new ArrayList<>();
        Repository repository = new RepositoryBhaskar(InstrumentationRegistry.getContext()) {
            @Override
            protected String getSubscriptionUrl(int pageNumber) {
                return String.format(Locale.US, DEFAULT_SUBSCRIPTION_URL, LOAD_SIZE, DEFAULT_CHANNEL, pageNumber);
            }
        };
        repository.setOnDataChangedListener(itemPojoList -> {
                    if (atomicInteger.intValue() == 0) {
                        Assert.assertEquals(LOAD_SIZE, itemPojoList.size());
                    }
                    if (atomicInteger.intValue() == 1) {
                        List<BhaskarItem> subList = itemPojoList.subList(0, LOAD_SIZE);
                        if (subList.equals(firstResult)) {
                            Assert.assertEquals(LOAD_SIZE * 2, itemPojoList.size());
                            countDownLatch2.countDown();
                        } else {
                            Assert.assertEquals(LOAD_SIZE, itemPojoList.size());
                            firstResult.addAll(itemPojoList);
                            countDownLatch1.countDown();
                        }
                    }
                    if (atomicInteger.intValue() == 2) {
                        Assert.assertEquals(LOAD_SIZE * 2, itemPojoList.size());
                        countDownLatch2.countDown();
                    }
                    atomicInteger.incrementAndGet();
                }
            );
        countDownLatch1.await();
        repository.loadMore();
        countDownLatch2.await();
    }
}
