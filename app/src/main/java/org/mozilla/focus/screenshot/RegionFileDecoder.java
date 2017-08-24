package org.mozilla.focus.screenshot;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by hart on 22/08/2017.
 */

public class RegionFileDecoder implements ResourceDecoder<InputStream, Bitmap> {
    private final BitmapPool bitmapPool;
    private final int defaultWidth;

    public RegionFileDecoder(Glide glide, int defaultWidth) {
        this(glide.getBitmapPool(), defaultWidth);
    }

    public RegionFileDecoder(BitmapPool bitmapPool, int defaultWidth) {
        this.bitmapPool = bitmapPool;
        this.defaultWidth = defaultWidth;
    }

    @Override
    public boolean handles(InputStream source, Options options) {
        return true;
    }

    @Override
    public Resource<Bitmap> decode(InputStream source, int width, int height, Options options) throws IOException {
        int imageWidth = defaultWidth;
        if (source.markSupported()) {
            BitmapFactory.Options optionSize = new BitmapFactory.Options();
            optionSize.inJustDecodeBounds = true;
            source.mark(source.available());
            BitmapFactory.decodeStream(source, new Rect(), optionSize);
            source.reset();
            imageWidth = optionSize.outWidth;
        }

        BitmapRegionDecoder decoder = createDecoder(source, width, height);
        BitmapFactory.Options opts = new BitmapFactory.Options();
        // Algorithm from Glide's Downsampler.getRoundedSampleSize
        int sampleSize = (int)Math.ceil((double)imageWidth / (double)width);
        sampleSize = sampleSize == 0 ? 0 : Integer.highestOneBit(sampleSize);
        sampleSize = Math.max(1, sampleSize);
        opts.inSampleSize = sampleSize;

        // Although functionally equivalent to 0 for BitmapFactory, 1 is a safer default for our code than 0.
        Bitmap bitmap = decoder.decodeRegion(new Rect(0, 0, imageWidth, imageWidth), opts);
        return BitmapResource.obtain(bitmap, bitmapPool);
    }

    private BitmapRegionDecoder createDecoder(InputStream source, int width, int height) throws IOException {
        return BitmapRegionDecoder.newInstance(source, false);
    }
}
