package org.mozilla.rocket.glide.transformation;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuffColorFilter;
import android.support.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class PorterDuffTransformation extends BitmapTransformation {
        private static Paint paint = new Paint();
        private static final String ID = "org.mozilla.PorterDuffTransformation";
        private static final byte[] ID_BYTES = ID.getBytes(StandardCharsets.UTF_8);
        private PorterDuffColorFilter porterDuffColorFilter;

        public PorterDuffTransformation(PorterDuffColorFilter porterDuffColorFilter) {
            this.porterDuffColorFilter = porterDuffColorFilter;
        }

        @Override
        protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform,
                                   int outWidth, int outHeight) {
            paint.setColorFilter(porterDuffColorFilter);
            Bitmap bitmap = pool.get(toTransform.getWidth(), toTransform.getHeight(), toTransform.getConfig());
            Canvas canvas = new Canvas(bitmap);
            canvas.drawBitmap(toTransform, 0, 0 , paint);
            return bitmap;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof PorterDuffTransformation;
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
