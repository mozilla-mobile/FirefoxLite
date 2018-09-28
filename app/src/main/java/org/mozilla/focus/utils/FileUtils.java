/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.Manifest;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.webkit.WebStorage;

import org.json.JSONObject;
import org.mozilla.rocket.util.LoggerWrapper;
import org.mozilla.threadutils.ThreadUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class FileUtils {
    public static final String WEBVIEW_DIRECTORY = "app_webview";
    private static final String WEBVIEW_CACHE_DIRECTORY = "cache";
    private static final String FAVICON_FOLDER_NAME = "favicons";


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

    public static boolean deleteDirectory(File directory) {
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

    public static void writeStringToFile(@NonNull final File dir,
                                         @NonNull final String fileName,
                                         @NonNull final String string) {
        ensureDir(dir);

        final File outputFile = new File(dir, fileName);
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {
            writer.write(string);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String readStringFromFile(@NonNull final File dir,
                                         @NonNull final String fileName) {
        ensureDir(dir);

        final File inputFile = new File(dir, fileName);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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

    public static class ReadStringFromFileTask<T> extends LiveDataTask<T, String> {
        private File dir;
        private String fileName;

        public ReadStringFromFileTask(File dir, String fileName, MutableLiveData<T> liveData, Function<T, String> function) {
            super(liveData, function);
            this.dir = dir;
            this.fileName = fileName;
        }

        @Override
        protected String doInBackground(Void... voids) {
            return readStringFromFile(dir, fileName);
        }
    }

    // An AsyncTask that do something in the background, and applies
    // a function before setting the Livedata.
    private static class LiveDataTask<T, S> extends AsyncTask<Void, Void, S> {

        public interface Function<T, S> {
            T apply(S source);
        }

        private MutableLiveData<T> liveData;
        private Function<T, S> function;

        protected LiveDataTask(MutableLiveData<T> liveData, Function<T, S> function) {
            this.liveData = liveData;
            this.function = function;
        }

        @Override
        protected void onPostExecute(S result) {
            liveData.setValue(function.apply(result));
        }

        @Override
        protected S doInBackground(Void... voids) {
            throw new IllegalStateException("LiveDataTask should not be instantiated");
        }
    }

    public static class WriteStringToFileRunnable extends FileIORunnable {

        private String string;

        public WriteStringToFileRunnable(File file, String string) {
            super(file);
            this.string = string;
        }

        @Override
        protected void doIO(File file) {
            writeStringToFile(file.getParentFile(), file.getName(), string);
        }
    }

    public static class DeleteFileRunnable extends FileIORunnable {

        public DeleteFileRunnable(File file) {
            super(file);
        }

        @Override
        protected void doIO(File file) {
            if (!file.exists()) {
                return;
            }
            if (!file.delete()) {
                LoggerWrapper.throwOrWarn("DeleteFileRunnable", "Failed to delete file");
            }
        }
    }

    public static class DeleteFolderRunnable extends FileIORunnable {

        public DeleteFolderRunnable(File directory) {
            super(directory);
        }

        @Override
        protected void doIO(File directory) {
            FileUtils.deleteContent(directory);
        }
    }

    private abstract static class FileIORunnable implements Runnable {
        private File file;

        private FileIORunnable(File file) {
            this.file = file;
        }

        @Override
        public void run() {
            doIO(file);
        }

        protected abstract void doIO(File file);
    }

    // context.getCacheDir() triggers strictMode, this is a workaround
    // function which, although, still blocks main thread, but can avoid
    // strictMode violation.
    public static class GetCache extends GetFile {

        public GetCache(WeakReference<Context> contextWeakReference) {
            super(contextWeakReference);
        }

        @Override
        protected File getFile(Context context) {
            return context.getCacheDir();
        }
    }

    private abstract static class GetFile {
        private Future<File> getFileFuture;

        private GetFile(WeakReference<Context> contextWeakReference) {
            getFileFuture = ThreadUtils.postToBackgroundThread(() -> {
                Context context = contextWeakReference.get();
                if (context == null) {
                    return null;
                }
                return getFile(context);
            });
        }

        protected abstract File getFile(Context context);

        public File get() throws ExecutionException, InterruptedException {
            return getFileFuture.get();
        }
    }

    // context.getFaviconFolder() triggers strictMode, this is a workaround
    // function which, although, still blocks main thread, but can avoid
    // strictMode violation.
    public static class GetFaviconFolder extends GetFile {

        public GetFaviconFolder(WeakReference<Context> contextWeakReference) {
            super(contextWeakReference);
        }

        @Override
        protected File getFile(Context context) {
            return getFaviconFolder(context);
        }
    }

    public static File getFaviconFolder(Context context) {
        File fileDir = context.getFilesDir();
        File faviconDir = new File(fileDir, FAVICON_FOLDER_NAME);
        if (!ensureDir(faviconDir)) {
            return context.getCacheDir();
        }
        return faviconDir;
    }

}
