package org.mozilla.rocket.util;

import org.mozilla.focus.utils.AppConstants;
import org.mozilla.logger.Logger;

public class LoggerWrapper {

    public static void throwOrWarn(String tag, String msg) {
        throwOrWarn(tag, msg, null);
    }

    public static void throwOrWarn(String tag, String msg, RuntimeException exception) {
        Logger.throwOrWarn(AppConstants.isBetaBuild(), tag, msg, exception);
    }
}
