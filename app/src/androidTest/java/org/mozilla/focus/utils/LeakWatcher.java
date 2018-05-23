package org.mozilla.focus.utils;

import android.app.Activity;

import java.lang.ref.WeakReference;

@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
        value = "DM_GC",
        justification = "This code is for testing only.")
public class LeakWatcher {
    private static volatile WeakReference<? extends Activity> reference;

    // this code is from LeakCanary
    public static void runGc() {
        // Code taken from AOSP FinalizationTest:
        // https://android.googlesource.com/platform/libcore/+/master/support/src/test/java/libcore/
        // java/lang/ref/FinalizationTester.java
        // System.gc() does not garbage collect every time. Runtime.gc() is
        // more likely to perfom a gc.
        Runtime.getRuntime().gc();
        enqueueReferences();
        System.runFinalization();
    }

    static private void enqueueReferences() {
        // Hack. We don't have a programmatic way to wait for the reference queue daemon to move
        // references to the appropriate queues.
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new AssertionError();
        }
    }

    public static void setReference(WeakReference<? extends Activity> ref) {
        LeakWatcher.reference = ref;
    }

    public static WeakReference<? extends Activity> getReference() {
        return LeakWatcher.reference;
    }
}
