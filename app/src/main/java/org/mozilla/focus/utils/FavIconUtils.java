package org.mozilla.focus.utils;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import org.mozilla.focus.R;

/**
 * Created by hart on 31/08/2017.
 */

public class FavIconUtils {
    public static Bitmap getRefinedBitmap(Resources res, Bitmap source, char initial) {
        if (source.getWidth() > res.getDimensionPixelSize(R.dimen.favicon_downscale_threshold_size)) {
            int targetSize = res.getDimensionPixelSize(R.dimen.favicon_target_size);
            return Bitmap.createScaledBitmap(source, targetSize, targetSize, false);
        } else if (source.getWidth() < res.getDimensionPixelSize(R.dimen.favicon_initial_threshold_size)) {
            return getInitialBitmap(res, source, initial);
        } else {
            return source;
        }
    }

    public static Bitmap getInitialBitmap(Resources res, Bitmap source, char initial) {
        char[] firstChar = {initial};
        int backgroundColor = getDominantColor(source);
        int textColor = getContractColor(backgroundColor);
        Paint paint = new Paint();
        paint.setTextSize(res.getDimension(R.dimen.favicon_initial_text_size));
        paint.setColor(textColor);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setAntiAlias(true);
        Rect bounds = new Rect();
        paint.getTextBounds(firstChar, 0, 1, bounds);

        final int size = res.getDimensionPixelSize(R.dimen.favicon_target_size);
        Bitmap image = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawColor(backgroundColor);
        canvas.drawText(firstChar, 0, 1, 0 + size / 2, 0 + size / 2 + bounds.height() / 2, paint);
        return image;
    }

    private static int getDominantColor(Bitmap source) {
        return getDominantColor(source, true);
    }

    private static int getDominantColor(Bitmap source, boolean applyThreshold) {
        if (source == null)
            return Color.argb(255, 255, 255, 255);

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
                if (Color.alpha(c) < 128)
                    continue;

                Color.colorToHSV(c, hsv);

                // If a threshold is applied, ignore arbitrarily chosen values for "white" and "black".
                if (applyThreshold && (hsv[1] <= 0.35f || hsv[2] <= 0.35f))
                    continue;

                // We compute the dominant color by putting colors in bins based on their hue.
                int bin = (int) Math.floor(hsv[0] / 10.0f);

                // Update the sum hue/saturation/value for this bin.
                sumHue[bin] = sumHue[bin] + hsv[0];
                sumSat[bin] = sumSat[bin] + hsv[1];
                sumVal[bin] = sumVal[bin] + hsv[2];

                // Increment the number of colors in this bin.
                colorBins[bin]++;

                // Keep track of the bin that holds the most colors.
                if (maxBin < 0 || colorBins[bin] > colorBins[maxBin])
                    maxBin = bin;
            }
        }

        // maxBin may never get updated if the image holds only transparent and/or black/white pixels.
        if (maxBin < 0)
            return Color.argb(255, 255, 255, 255);

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
}
