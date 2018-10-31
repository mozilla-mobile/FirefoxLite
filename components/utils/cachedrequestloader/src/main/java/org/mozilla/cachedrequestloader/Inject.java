package org.mozilla.cachedrequestloader;

public class Inject {

    static void sleepIfTesting(boolean shouldSleep) {
        // Do nothing on non-test flavors
    }

    static void postDelayIfTesting(Runnable runnable, boolean shouldDelay) {
        runnable.run();
    }
}
