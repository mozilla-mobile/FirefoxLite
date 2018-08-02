package org.mozilla.rocket.util;

import android.util.Log;

import org.mozilla.focus.utils.AppConstants;

public class Logger {
    public static void throwOrWarn(String tag, String msg) {
        if (AppConstants.isReleaseBuild()) {
            Log.e(tag, msg);
        } else {
            throw new IllegalArgumentException(msg);
        }
    }
}
