package org.mozilla.logger;

import android.util.Log;

public class Logger {

    public static void throwOrWarn(boolean logOnly, String tag, String msg, RuntimeException exception) {
        if (logOnly) {
            Log.e(tag, msg);
        } else {
            if (exception == null) {
                throw new RuntimeException(msg);
            } else {
                throw exception;
            }
        }
    }
}
