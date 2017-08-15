package org.mozilla.focus.provider;

import android.net.Uri;
import android.provider.BaseColumns;

import org.mozilla.focus.BuildConfig;

/**
 * Created by hart on 15/08/2017.
 */

public class ScreenshotContract {

    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".provider.screenshotprovider";
    public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

    public static final class Screenshot implements BaseColumns {

        private Screenshot() {}

        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "screenshot");

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd." + BuildConfig.APPLICATION_ID + ".provider.screenshotprovider.screenshot";

        public static final String TITLE = "title";
        public static final String URL = "url";
        public static final String TIMESTAMP = "timestamp";
        public static final String IMAGE_URI = "image_uri";
    }
}
