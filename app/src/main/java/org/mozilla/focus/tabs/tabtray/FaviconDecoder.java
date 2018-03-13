/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.tabs.tabtray;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;

import org.mozilla.focus.utils.FavIconUtils;

import java.io.IOException;

public class FaviconDecoder implements ResourceDecoder<FaviconModel, Bitmap> {
    private Context context;
    private Glide glide;

    public FaviconDecoder(Context context, Glide glide) {
        this.context = context;
        this.glide = glide;
    }

    @Override
    public boolean handles(FaviconModel source, Options options) throws IOException {
        return true;
    }

    @Nullable
    @Override
    public Resource<Bitmap> decode(FaviconModel source, int width, int height, Options options) throws IOException {
        Bitmap refinedBitmap  = FavIconUtils.getRefinedBitmap(context.getResources(), source.originalIcon,
                FavIconUtils.getRepresentativeCharacter(source.url));
        return BitmapResource.obtain(refinedBitmap, glide.getBitmapPool());
    }
}
