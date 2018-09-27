package org.mozilla.rocket.glide.transformation;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import androidx.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class ShrinkSizeTransformation extends BitmapTransformation {
        private static Paint paint = new Paint();
        private static final String ID = "org.mozilla.PorterDuffTransformation";
        private static final byte[] ID_BYTES = ID.getBytes(StandardCharsets.UTF_8);
        private double scale;

        public ShrinkSizeTransformation(double scale) {
            if (scale >= 1) {
                throw new IllegalArgumentException("Scale should be <= 1");
            }
            this.scale = scale;
        }

        private int margin(int size, double scale) {
            return (int) ( size * ((1 / scale) - 1) / 2);
        }

        @Override
        protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform,
                                   int outWidth, int outHeight) {
            Bitmap bitmap = pool.get((int) (toTransform.getWidth() / scale), (int) (toTransform.getHeight() / scale), toTransform.getConfig());
            Canvas canvas = new Canvas(bitmap);
            canvas.drawBitmap(toTransform, margin(toTransform.getWidth(), scale), margin(toTransform.getHeight(), scale) , paint);
            return bitmap;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof ShrinkSizeTransformation;
        }

        @Override
        public int hashCode() {
            return ID.hashCode();
        }

        @Override
        public void updateDiskCacheKey(MessageDigest messageDigest) {
            messageDigest.update(ID_BYTES);
        }
    }
