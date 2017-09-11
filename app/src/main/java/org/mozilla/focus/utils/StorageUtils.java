/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.mozilla.focus.R;

import java.io.File;

public class StorageUtils {

    final static String DOWNLOAD_DIR = "downloads";
    final static String IMAGE_DIR = "pictures";
    final static String OTHER_DIR = "others";

    /**
     * Test if we have a removable storage and throw Exception if no is available and specify cause.
     * Note that exception is thrown even if the user does not want to save to removable storage.
     *
     * @param ctx  Context
     * @return app-owned-directory on removable storage
     * @throws NoRemovableStorageException if there is no removable storage to use.
     */
    private static File getAppMediaDirOnRemovableStorage(@NonNull Context ctx)
            throws NoRemovableStorageException {

        final File media = getFirstRemovableMedia(ctx);
        if (media == null) {
            throw new NoRemovableStorageException("No removable media to use");
        }

        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState(media))) {
            throw new NoRemovableStorageException("No mounted-removable media to use");
        }

        return media;
    }

    /**
     * To get a directory to save downloaded file. Before invoke this method, callee should check we
     * already granted necessary permission.
     *
     * @param ctx  Context
     * @param type should be Download.TYPE_IMAGE, Download.TYPE_OTHER
     * @return a directory on removable storage to save files. return null if user's preference is off.
     * @throws NoRemovableStorageException if user's preference is on, but there is no removable storage to use.
     */
    public static File getTargetDirOnRemovableStorageForDownloads(@NonNull Context ctx, String type)
            throws NoRemovableStorageException {

        if (!Settings.getInstance(ctx).shouldSaveToRemovableStorage()) {
            return null;
        }

        final File media = getAppMediaDirOnRemovableStorage(ctx);

        final File dir = new File(media, DOWNLOAD_DIR);

        if (MimeUtils.isImage(type)) {
            return new File(dir, IMAGE_DIR);
        } else {
            return new File(dir, OTHER_DIR);
        }
    }

    public static File getTargetDirForSaveScreenshot(@NonNull Context ctx) {
        String folderName = ctx.getString(R.string.app_name).replaceAll(" ","");
        File defaultDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), folderName);
        FileUtils.ensureDir(defaultDirectory);

        if (!Settings.getInstance(ctx).shouldSaveToRemovableStorage()) {
            return defaultDirectory;
        }

        try {
            return new File(getAppMediaDirOnRemovableStorage(ctx), folderName);
        } catch (NoRemovableStorageException ex) {
            return defaultDirectory;
        }
    }

    @VisibleForTesting
    static File getFirstRemovableMedia(@NonNull Context ctx) {
        final File[] files = ctx.getExternalMediaDirs();
        for (final File file : files) {
            // on some devices such as Oppo, it might return null
            if ((file != null) && Environment.isExternalStorageRemovable(file)) {
                return file;
            }
        }
        return null;
    }
}
