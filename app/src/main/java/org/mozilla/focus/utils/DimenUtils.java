package org.mozilla.focus.utils;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.annotation.IntDef;

import org.mozilla.focus.R;
import org.mozilla.icon.FavIconUtils;

public class DimenUtils {

    public static final int TYPE_ORIGINAL = 0;
    public static final int TYPE_SCALED_DOWN = 1;
    public static final int TYPE_GENERATED = 2;
    public static final int JPEG_QUALITY = 80;

    @IntDef({TYPE_ORIGINAL, TYPE_SCALED_DOWN, TYPE_GENERATED})
    public @interface FavIconType {
    }

    /**
     * @deprecated We're giving up this methodology. The only left use case is Tab Tray which should
     * be migrated in the future too. Please consider {@link #iconTooBlurry(Resources, int)}
     * as a replacement.
     */
    @Deprecated
    @FavIconType
    public static int getFavIconType(Resources res, Bitmap source) {
        if (source == null || source.getWidth() < res.getDimensionPixelSize(R.dimen.favicon_initial_threshold_size)) {
            return TYPE_GENERATED;
        }

        if (source.getWidth() > res.getDimensionPixelSize(R.dimen.favicon_downscale_threshold_size)) {
            return TYPE_SCALED_DOWN;
        }

        return TYPE_ORIGINAL;
    }

    /**
     * @deprecated We're giving up this methodology. The only left use case is Tab Tray which should
     * be migrated in the future too. Please consider {@link #iconTooBlurry(Resources, int)}
     * as a replacement.
     */
    @Deprecated
    public static Bitmap getRefinedBitmap(Resources res, Bitmap source, char initial) {
        switch (getFavIconType(res, source)) {
            case TYPE_ORIGINAL:
                return source;

            case TYPE_SCALED_DOWN:
                int targetSize = res.getDimensionPixelSize(R.dimen.favicon_target_size);
                return Bitmap.createScaledBitmap(source, targetSize, targetSize, false);

            case TYPE_GENERATED:
                return DimenUtils.getInitialBitmap(res, source, initial);

            default:
                return DimenUtils.getInitialBitmap(res, source, initial);
        }
    }

    public static boolean iconTooBlurry(Resources resources, int bitmapWidth) {
        return bitmapWidth < resources.getDimensionPixelSize(R.dimen.favicon_initial_threshold_size);
    }

    private static float getDefaultFaviconTextSize(Resources resources) {
        return resources.getDimension(R.dimen.favicon_initial_text_size);
    }

    private static int getDefaultFaviconBitmapSize(Resources resources) {
        return resources.getDimensionPixelSize(R.dimen.favicon_target_size);
    }

    public static Bitmap getInitialBitmap(Resources resources, Bitmap source, char initial) {
        return FavIconUtils.getInitialBitmap(source, initial, getDefaultFaviconTextSize(resources), getDefaultFaviconBitmapSize(resources));
    }

    public static Bitmap getInitialBitmap(Resources resources, char initial, int backgroundColor) {
        return FavIconUtils.getInitialBitmap(initial, backgroundColor, getDefaultFaviconTextSize(resources), getDefaultFaviconBitmapSize(resources));
    }

    static Bitmap getRefinedShortcutIcon(Resources res, Bitmap source, char initial) {
        final int sizeThreshold = res.getDimensionPixelSize(R.dimen.shortcut_icon_size);

        if (source == null || source.getWidth() < sizeThreshold) {
            return DimenUtils.getInitialBitmap(res, source, initial);
        }
        if (source.getWidth() > sizeThreshold) {
            return Bitmap.createScaledBitmap(source, sizeThreshold, sizeThreshold, false);
        } else {
            return source;
        }
    }
}
