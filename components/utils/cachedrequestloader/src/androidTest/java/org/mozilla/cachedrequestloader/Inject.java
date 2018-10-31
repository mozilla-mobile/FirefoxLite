package org.mozilla.cachedrequestloader;

import org.mozilla.threadutils.ThreadUtils;

public class Inject {

    static private final long TEST_DELAY = 300;

    static void sleepIfTesting(boolean shouldSleep) {
        if (!shouldSleep) {
            return;
        }
        try {
            Thread.sleep(TEST_DELAY);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static void postDelayIfTesting(Runnable runnable, boolean shouldDelay) {
        if (shouldDelay) {
            ThreadUtils.postToMainThreadDelayed(runnable, TEST_DELAY);
        } else {
            runnable.run();
        }
    }
}
