package org.mozilla.focus.test.runner;

import android.os.Bundle;
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
}