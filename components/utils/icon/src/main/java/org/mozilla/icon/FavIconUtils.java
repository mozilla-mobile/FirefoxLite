/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.icon;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import android.text.TextUtils;

import org.mozilla.urlutils.UrlUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by hart on 31/08/2017.
 */

public class FavIconUtils {

    public static Bitmap getInitialBitmap(Bitmap source, char initial, float textSize, int bitmapSize) {
        return getInitialBitmap(initial, getDominantColor(source), textSize, bitmapSize);
    }

    public static Bitmap getInitialBitmap(char initial, int backgroundColor, float textSize, int bitmapSize) {
        char[] firstChar = {initial};
        int textColor = getContractColor(backgroundColor);
        Paint paint = new Paint();
        paint.setTextSize(textSize);
        paint.setColor(textColor);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setAntiAlias(true);
        Rect bounds = new Rect();
        paint.getTextBounds(firstChar, 0, 1, bounds);

        Bitmap image = Bitmap.createBitmap(bitmapSize, bitmapSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawColor(backgroundColor);
        canvas.drawText(firstChar, 0, 1, (float) (bitmapSize) / 2, (float) (bitmapSize + bounds.height()) / 2, paint);
        return image;
    }

    public static int getDominantColor(Bitmap source) {
        return getDominantColor(source, true);
    }

    private static int getDominantColor(Bitmap source, boolean applyThreshold) {
        if (source == null) {
            return Color.argb(255, 255, 255, 255);
        }

        // Keep track of how many times a hue in a given bin appears in the image.
        // Hue values range [0 .. 360), so dividing by 10, we get 36 bins.
        int[] colorBins = new int[36];

        // The bin with the most colors. Initialize to -1 to prevent accidentally
        // thinking the first bin holds the dominant color.
        int maxBin = -1;

        // Keep track of sum hue/saturation/value per hue bin, which we'll use to
        // compute an average to for the dominant color.
        float[] sumHue = new float[36];
        float[] sumSat = new float[36];
        float[] sumVal = new float[36];
        float[] hsv = new float[3];

        int height = source.getHeight();
        int width = source.getWidth();
        int[] pixels = new int[width * height];
        source.getPixels(pixels, 0, width, 0, 0, width, height);
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int c = pixels[col + row * width];
                // Ignore pixels with a certain transparency.
                if (Color.alpha(c) < 128) {
                    continue;
                }

                Color.colorToHSV(c, hsv);

                // If a threshold is applied, ignore arbitrarily chosen values for "white" and "black".
                if (applyThreshold && (hsv[1] <= 0.35f || hsv[2] <= 0.35f)) {
                    continue;
                }

                // We compute the dominant color by putting colors in bins based on their hue.
                int bin = (int) Math.floor(hsv[0] / 10.0f);

                // Update the sum hue/saturation/value for this bin.
                sumHue[bin] = sumHue[bin] + hsv[0];
                sumSat[bin] = sumSat[bin] + hsv[1];
                sumVal[bin] = sumVal[bin] + hsv[2];

                // Increment the number of colors in this bin.
                colorBins[bin]++;

                // Keep track of the bin that holds the most colors.
                if (maxBin < 0 || colorBins[bin] > colorBins[maxBin]) {
                    maxBin = bin;
                }
            }
        }

        // maxBin may never get updated if the image holds only transparent and/or black/white pixels.
        if (maxBin < 0) {
            return Color.argb(255, 255, 255, 255);
        }

        // Return a color with the average hue/saturation/value of the bin with the most colors.
        hsv[0] = sumHue[maxBin] / colorBins[maxBin];
        hsv[1] = sumSat[maxBin] / colorBins[maxBin];
        hsv[2] = sumVal[maxBin] / colorBins[maxBin];
        return Color.HSVToColor(hsv);
    }

    private static int getContractColor(int color) {
        // Counting the perceptive luminance - human eye favors green color...
        double a = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return a < 0.5 ? Color.BLACK : Color.WHITE;
    }

    /**
     * Get a representative character for the given URL.
     * <p>
     * For example this method will return "f" for "http://m.facebook.com/foobar".
     */
    public static char getRepresentativeCharacter(String url) {
        if (TextUtils.isEmpty(url)) {
            return '?';
        }

        final String snippet = getRepresentativeSnippet(url);
        for (int i = 0; i < snippet.length(); i++) {
            char c = snippet.charAt(i);

            if (Character.isLetterOrDigit(c)) {
                return Character.toUpperCase(c);
            }
        }

        // Nothing found..
        return '?';
    }

    /**
     * Get the representative part of the URL. Usually this is the host (without common prefixes).
     */
    private static String getRepresentativeSnippet(@NonNull String url) {
        Uri uri = Uri.parse(url);

        // Use the host if available
        String snippet = uri.getHost();

        if (TextUtils.isEmpty(snippet)) {
            // If the uri does not have a host (e.g. file:// uris) then use the path
            snippet = uri.getPath();
        }

        if (TextUtils.isEmpty(snippet)) {
            // If we still have no snippet then just return the question mark
            return "?";
        }

        // Strip common prefixes that we do not want to use to determine the representative characterS
        snippet = UrlUtils.stripCommonSubdomains(snippet);

        return snippet;
    }

    public interface Consumer<T> {
        void accept(T arg);
    }

    public static class SaveBitmapTask extends AsyncTask<Void, Void, String> {

        private File directory;
        private String url;
        private Bitmap bitmap;
        private Consumer<String> callback;
        private final Bitmap.CompressFormat compressFormat;
        private final int quality;


        public SaveBitmapTask(File directory, String url, Bitmap bitmap, Consumer<String> callback,
                              Bitmap.CompressFormat compressFormat, int quality) {
            this.directory = directory;
            this.url = url;
            this.bitmap = bitmap;
            this.callback = callback;
            this.compressFormat = compressFormat;
            this.quality = quality;
        }

        @Override
        protected void onPostExecute(String result) {
            callback.accept(result);
        }

        @Override
        protected String doInBackground(Void... voids) {
            return saveBitmapToDirectory(directory, url, bitmap, compressFormat, quality);
        }
    }

    public static class SaveBitmapsTask extends AsyncTask<Void, Void, List<String>> {

        private File directory;
        private List<String> urls;
        private List<byte[]> bytesList;
        private FavIconUtils.Consumer<List<String>> callback;
        private final Bitmap.CompressFormat compressFormat;
        private final int quality;

        public SaveBitmapsTask(File directory, List<String> urls, List<byte[]> bytesList,
                               FavIconUtils.Consumer<List<String>> callback,
                               Bitmap.CompressFormat compressFormat, int quality) {
            this.directory = directory;
            this.urls = urls;
            this.bytesList = bytesList;
            this.callback = callback;
            this.compressFormat = compressFormat;
            this.quality = quality;
        }

        @Override
        protected void onPostExecute(List<String> result) {
            callback.accept(result);
        }

        @Override
        protected List<String> doInBackground(Void... voids) {
            List<String> ret = new ArrayList<>();
            for (int i = 0 ; i < urls.size() ; i++) {
                byte[] bytes = bytesList.get(i);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                ret.add(FavIconUtils.saveBitmapToDirectory(directory, urls.get(i), bitmap,
                        compressFormat, quality));
            }
            return ret;
        }
    }

    public static String saveBitmapToDirectory(@NonNull final File dir,
                                               @NonNull final String url,
                                               @NonNull final Bitmap bitmap,
                                               @NonNull final Bitmap.CompressFormat compressFormat,
                                               final int quality) {
        // Use encoded url as default if No MD5 algorithm
        String fileName = Uri.encode(url);
        try {
            fileName = generateMD5(url);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return FavIconUtils.saveBitmapToFile(dir, fileName, bitmap, compressFormat, quality);
    }

    private static String saveBitmapToFile(@NonNull final File dir,
                                           @NonNull final String fileName,
                                           @NonNull final Bitmap bitmap,
                                           @NonNull final Bitmap.CompressFormat compressFormat,
                                           final int quality) {
        ensureDir(dir);

        try {
            //create a file to write bitmap data
            File file = new File(dir, fileName);
            if (file.exists() && !file.delete()) {
                return Uri.fromFile(file).toString();
            }
            if (!file.createNewFile()) {
               if (file.exists()) {
                   return Uri.fromFile(file).toString();
               } else {
                   return null;
               }
            }
            //Convert bitmap to byte array
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(compressFormat, quality, bos);
            byte[] bitmapData = bos.toByteArray();

            //write the bytes in file
            try (FileOutputStream fos = new FileOutputStream(file) ) {
                fos.write(bitmapData);
                fos.flush();
            }
            return Uri.fromFile(file).toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Bitmap getIconFromAssets(Context context, String path) {
        AssetManager assetManager = context.getAssets();

        InputStream istream;
        Bitmap bitmap = null;
        try {
            istream = assetManager.open(path);
            bitmap = BitmapFactory.decodeStream(istream);
        } catch (IOException e) {
            // handle exception
        }

        return bitmap;

    }


    public static Bitmap getBitmapFromUri(Context context, @NonNull final String uri) {
        final String assetIndicator = "//android_asset/";
        if (uri.contains(assetIndicator)) {
            return getIconFromAssets(context, uri.substring(uri.indexOf(assetIndicator) + assetIndicator.length()));
        }
        return BitmapFactory.decodeFile(Uri.parse(uri).getPath(), new BitmapFactory.Options());
    }

    public static String generateMD5(String string) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        messageDigest.update(string.getBytes(Charset.defaultCharset()));
        byte[] result = messageDigest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte character : result) {
            sb.append(String.format("%02X", character));
        }
        return sb.toString();
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
}
