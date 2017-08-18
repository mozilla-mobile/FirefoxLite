/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.content.Context;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils {
    private static final String WEBVIEW_DIRECTORY = "app_webview";
    private static final String WEBVIEW_CACHE_DIRECTORY = "cache";

    public static boolean truncateCacheDirectory(final Context context) {
        final File cacheDirectory = context.getCacheDir();
        return cacheDirectory.exists() && deleteContent(cacheDirectory);
    }

    public static boolean deleteWebViewDirectory(final Context context) {
        final File webviewDirectory = new File(context.getApplicationInfo().dataDir, WEBVIEW_DIRECTORY);
        return webviewDirectory.exists() && deleteDirectory(webviewDirectory);
    }

    public static long deleteWebViewCacheDirectory(final Context context) {
        final File cachedir = new File(context.getApplicationInfo().dataDir, WEBVIEW_CACHE_DIRECTORY);
        if (!cachedir.exists()) {
            return -1L;
        }
        return deleteContentOnly(cachedir);
    }

    /**
     * To ensure a directory exists and writable
     *
     * @param dir directory as File type
     * @return true if the directory is writable
     */
    public static boolean ensureDir(@NonNull File dir) {
        if (dir.mkdirs()) {
            return true;
        } else {
            return dir.exists() && dir.isDirectory() && dir.canWrite();
        }
    }

    /**
     * To copy a file from src to dst.
     *
     * @param src source file to read-from.
     * @param dst destination file to write-in. NOT A DIRECTORY
     * @return true if copy successful
     */
    public static boolean copy(@NonNull File src, @NonNull File dst) {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            if (dst.exists()) {
                return false;
            }

            fis = new FileInputStream(src);
            fos = new FileOutputStream(dst);
            boolean result = copy(fis, fos);
            fis.close();
            fos.close();
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    public static boolean copy(@NonNull InputStream src, @NonNull OutputStream dst) {
        final byte[] buffer = new byte[1024];
        try {
            int read;
            while ((read = src.read(buffer)) != -1) {
                dst.write(buffer, 0, read);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private static boolean deleteDirectory(File directory) {
        return deleteContent(directory) && directory.delete();
    }

    private static boolean deleteContent(File directory) {
        boolean success = true;

        final String[] files = directory.list();
        if (files == null) {
            return false;
        }

        for (final String name : files) {
            final File file = new File(directory, name);
            if (file.isDirectory()) {
                success &= deleteDirectory(file);
            } else {
                success &= file.delete();
            }
        }

        return success;
    }

    private static long deleteContentOnly(File directory) {
        long deleted = 0;

        final String[] files = directory.list();
        if (files == null) {
            return deleted;
        }

        for (final String name : files) {
            final File file = new File(directory, name);
            if (file.isDirectory()) {
                deleted += deleteContentOnly(file);
            } else {
                long length = file.length();
                if (file.delete()) {
                    deleted += length;
                }
            }
        }

        return deleted;
    }
}
