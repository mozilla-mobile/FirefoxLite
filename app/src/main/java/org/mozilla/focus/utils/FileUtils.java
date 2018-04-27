/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.webkit.WebStorage;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;

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

    private static long deleteWebViewCacheDirectory(final Context context) {
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

    /**
     * To get a file which does not exist yet, to ensure file name collision.
     *
     * @param dir      The directory to check from
     * @param fileName Suggest file name
     * @return a File which definitely does not exist
     */
    public static File getFileSlot(@NonNull File dir, @NonNull String fileName) {
        File target = new File(dir, fileName);
        if (!target.exists()) {
            return target;
        }

        // If target file existed, prepend a serial number to file name, up to 1000
        for (int i = 1; i < 1000; i++) {
            target = new File(dir, i + "-" + fileName);
            if (!target.exists()) {
                return target;
            }
        }

        return getFileSlot(dir, "Not-lucky-" + fileName); // recursive until we make it!
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

    public static void notifyMediaScanner(Context context, String path) {
        MediaScannerConnection.scanFile(context, new String[]{path}, new String[]{null}, null);
    }

    public static long clearCache(Context context) {
        WebStorage.getInstance().deleteAllData();
        return FileUtils.deleteWebViewCacheDirectory(context);
    }

    public static void writeBundleToStorage(@NonNull final File dir,
                                            @NonNull final String fileName,
                                            @NonNull final Bundle bundle) {
        ensureDir(dir);

        final File outputFile = new File(dir, fileName);
        FileOutputStream fos;
        ObjectOutputStream oos = null;
        try {
            fos = new FileOutputStream(outputFile);
            oos = new ObjectOutputStream(fos);
            new AndroidBundleSerializer().serializeBundle(oos, bundle);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (oos != null) {
                    oos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static Bundle readBundleFromStorage(@NonNull final File dir,
                                               @NonNull final String fileName) {
        ensureDir(dir);

        final File input = new File(dir, fileName);
        if (!input.exists()) {
            return null;
        }

        Bundle bundle = null;
        try (FileInputStream fis = new FileInputStream(input); ObjectInputStream ois = new ObjectInputStream(fis)) {
            bundle = new AndroidBundleSerializer().deserializeBundle(ois);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bundle;
    }

    // Assume we already have read external storage permission
    // For any Exception, we'll handle them in the same way. So a general Exception should be fine.
    public static HashMap<String, Object> fromJsonOnDisk(String remoteConfigJson) throws Exception {

        final File sdcard = Environment.getExternalStorageDirectory();

        // Check External Storage
        if (sdcard == null) {
            throw new Exception("No External Storage Available");
        }

        // Check if config file exist
        final File file = new File(sdcard, remoteConfigJson);

        if (!file.exists()) {
            throw new FileNotFoundException("Can't find " + remoteConfigJson);
        }

        // Read text from config file
        final StringBuilder text = new StringBuilder();


        // try with resource so br will call close() automatically
        try (InputStream inputStream = new FileInputStream(file);
             BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
        }

        // Parse JSON and put it into a HashMap
        final JSONObject jsonObject = new JSONObject(text.toString());
        final HashMap<String, Object> map = new HashMap<>();

        // Iterate the JSON and put the key-value pair in the HashMap
        for (Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
            String key = it.next();
            map.put(key, jsonObject.get(key));
        }


        return map;
    }

    // Check if we have the permission.
    public static boolean canReadExternalStorage(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int result = context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            return result == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

}
