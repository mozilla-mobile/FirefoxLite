/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

public class Constants {

    /**
     * Activity action, to prompt dialog to grant permission
     */
    public static final String ACTION_REQUEST_PERMISSION = "org.mozilla.action.REQUEST_PERMISSION";

    /**
     * Activity action, to show a message to user
     */
    public static final String ACTION_NOTIFY_UI = "org.mozilla.action.NOTIFY_UI";

    /**
     * Activity action, to show download finished SnackBar
     */
    public static final String ACTION_NOTIFY_RELOCATE_FINISH = "org.mozilla.action.RELOCATE_FINISH";

    /**
     * To indicate this intent is created when we were operating file
     */
    public static final String CATEGORY_FILE_OPERATION = "org.mozilla.category.FILE_OPERATION";

    /**
     * Option value for download id comes from Download Manager. Its value type should be *long*
     */
    public static final String EXTRA_DOWNLOAD_ID = "org.mozilla.extra.download_id";

    /**
     * The row id of our DownloadInfoManager. Its value type should be *long*
     */
    public static final String EXTRA_ROW_ID = "org.mozilla.extra.row_id";


    /**
     * Option value for file path. Its value type should be *string*
     */
    public static final String EXTRA_FILE_PATH = "org.mozilla.extra.file_path";

    /**
     * Option value as message. Its value type should be *CharSequence*
     */
    public static final String EXTRA_MESSAGE = "org.mozilla.extra.message";

    private Constants() {
        throw new RuntimeException("How dare you try to initialize Constants");
    }
}
