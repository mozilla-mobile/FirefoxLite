/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.mozilla.focus.web.Download;

import java.io.File;

public class StorageUtils {

    final static String DOWNLOAD_DIR = "downloads";
    final static String IMAGE_DIR = "pictures";
    final static String OTHER_DIR = "others";

    /**
     * To get a directory to save downloaded file. Before invoke this method, callee should check we
     * already granted necessary permission.
     *
     * @param ctx  Context
     * @param type should be Download.TYPE_IMAGE, Download.TYPE_OTHER
     * @return a directory on removable storage to save files. return null if user's preference is off.
     * @throws NoRemovableStorageException if user's preference is on, but there is no removable storage to use.
     */
    public static File getTargetDirOnRemovableStorage(@NonNull Context ctx, int type)
            throws NoRemovableStorageException {

        if (!Settings.getInstance(ctx).shouldSaveToRemovableStorage()) {
            return null;
        }

        final File media = getFirstRemovableMedia(ctx);
        if (media == null) {
            throw new NoRemovableStorageException("No removable media to use");
        }

        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState(media))) {
            throw new NoRemovableStorageException("No mounted-removable media to use");
        }

        final File dir = new File(media, DOWNLOAD_DIR);

        switch (type) {
            case Download.TYPE_IMAGE:
                return new File(dir, IMAGE_DIR);
            case Download.TYPE_OTHER:
                return new File(dir, OTHER_DIR);
            default:
                throw new IllegalArgumentException("Unknown type");
        }
    }

    @VisibleForTesting
    static File getFirstRemovableMedia(@NonNull Context ctx) {
        final File[] files = ctx.getExternalMediaDirs();
        for (final File file : files) {
            if (Environment.isExternalStorageRemovable(file)) {
                return file;
            }
        }
        return null;
    }
}
