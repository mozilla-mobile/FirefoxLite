package org.mozilla.focus.utils;

/**
 * Created by hart on 15/08/2017.
 */

public class ProviderUtils {
    public static String getLimitParam(String offset, String limit) {
        return (limit == null) ? null : (offset == null) ? limit : offset + "," + limit;
    }
}
