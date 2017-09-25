/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.screenshot;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.text.TextUtils;

import org.mozilla.focus.screenshot.model.Screenshot;
import org.mozilla.focus.utils.FileUtils;
import org.mozilla.focus.utils.StorageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ScreenshotCaptureTask extends AsyncTask<Object, Void, String> {

    private final Context context;

    public ScreenshotCaptureTask(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    protected String doInBackground(Object... params) {
        String title = (String) params[0];
        String url = (String) params[1];
        Bitmap content = (Bitmap) params[2];
        long timestamp = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

        try {
            final String path = saveBitmapToStorage(context, "Screenshot_" + sdf.format(new Date(timestamp)), content);
            // Failed to save
            if (!TextUtils.isEmpty(path)) {
                FileUtils.notifyMediaScanner(context, path);

                Screenshot screenshot = new Screenshot(title, url, timestamp, path);
                ScreenshotManager.getInstance().insert(screenshot, null);
            }

            return path;
        } catch (IOException ex) {
            return null;
        }
    }
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
            value="RV_RETURN_VALUE_IGNORED_BAD_PRACTICE",
            justification="We have nothing to do when the delete fails.")
    private static String saveBitmapToStorage(Context context, String fileName, Bitmap bitmap) throws IOException {
        File folder = StorageUtils.getTargetDirForSaveScreenshot(context);
        if (!FileUtils.ensureDir(folder)) {
            throw new IOException("Can't create folder");
        }
        String path = null;
        fileName = fileName.concat(".png");

        File file = new File(folder, fileName);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            if(bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)) {
                fos.flush();
                path = file.getPath();
            } else {
                file.delete();
                path = null;
            }
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }
        return path;
    }

}

