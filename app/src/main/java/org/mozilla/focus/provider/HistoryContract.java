/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.provider;

import android.net.Uri;
import android.provider.BaseColumns;

import org.mozilla.focus.BuildConfig;

/**
 * Created by hart on 03/08/2017.
 */

public class HistoryContract {

    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".provider.historyprovider";
    public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);
    public static final String TABLE_NAME = "browsing_history";

    public static final class BrowsingHistory implements BaseColumns {

        private BrowsingHistory() {
        }

        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, TABLE_NAME);

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd." + BuildConfig.APPLICATION_ID + ".provider.historyprovider.browsinghistory";

        public static final String TITLE = "title";
        public static final String URL = "url";
        public static final String VIEW_COUNT = "view_count";
        public static final String LAST_VIEW_TIMESTAMP = "last_view_timestamp";
        // v1
        public static final String FAV_ICON = "fav_icon";
        // v2
        public static final String FAV_ICON_URI = "fav_icon_uri";
    }
}
