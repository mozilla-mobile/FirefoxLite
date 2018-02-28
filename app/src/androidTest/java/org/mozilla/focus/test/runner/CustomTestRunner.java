package org.mozilla.focus.test.runner;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.test.runner.AndroidJUnitRunner;

public class CustomTestRunner extends AndroidJUnitRunner {
    @Override
    public void onCreate(final Bundle arguments) {
        // The strict mode exception throws while running instrumentation tests on Android O.
        // Please refer to the following gist for the crash stack:
        // https://gist.github.com/benjamin-cheng/cdc8db18746b613067fd62dfe30644cc

        // It happened after the espresso test was finished and the test runner try to send the tracking data back to its server.
        // However, the network call is not tagged which is required by the strict mode on Android O device. then the exception was thrown.
        // So, the workaround is to disable the analytics in test runner and refer to the following post:
        // http://izmajlowiczl.blogspot.tw/2014/08/espresso-and-hidden-analytics-calls.html
        arguments.putString("disableAnalytics", "true");
        super.onCreate(arguments);
    }

    @Override
    public void onStart() {
        runOnMainSync(new Runnable() {
            @Override
            public void run() {
                Context applicationContext = CustomTestRunner.this.getTargetContext().getApplicationContext();

                String tag = CustomTestRunner.class.getSimpleName();
                unlockScreen(applicationContext, tag);
                keepScreenAwake(applicationContext, tag);
            }
        });

        super.onStart();
    }

    private void keepScreenAwake(Context applicationContext, String name) {
        PowerManager power = (PowerManager) applicationContext.getSystemService(Context.POWER_SERVICE);
        power.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, name).acquire();
    }

    private void unlockScreen(Context applicationContext, String name) {
        KeyguardManager keyguard = (KeyguardManager) applicationContext.getSystemService(Context.KEYGUARD_SERVICE);
        keyguard.newKeyguardLock(name).disableKeyguard();
    }
}