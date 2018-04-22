/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.tabs.tabtray;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.signature.ObjectKey;

public class FaviconModelLoaderFactory implements ModelLoaderFactory<FaviconModel, FaviconModel> {

    @Override
    public ModelLoader<FaviconModel, FaviconModel> build(MultiModelLoaderFactory multiFactory) {
        return new FaviconModelLoader();
    }

    @Override
    public void teardown() {
    }

    public static class FaviconModelLoader implements ModelLoader<FaviconModel, FaviconModel> {

        @Nullable
        @Override
        public LoadData<FaviconModel> buildLoadData(FaviconModel model, int width, int height, Options options) {
            return new LoadData<>(new ObjectKey(model.url), new Fetcher(model));
        }

        @Override
        public boolean handles(FaviconModel faviconModel) {
            return true;
        }
    }

    public static class Fetcher implements DataFetcher<FaviconModel> {
        private final FaviconModel model;

        Fetcher(FaviconModel model) {
            this.model = model;
        }

        @Override
        public void loadData(Priority priority, DataCallback<? super FaviconModel> callback) {
            callback.onDataReady(model);
        }

        @Override
        public void cleanup() {
        }

        @Override
        public void cancel() {
        }

        @NonNull
        @Override
        public Class<FaviconModel> getDataClass() {
            return FaviconModel.class;
        }

        @NonNull
        @Override
        public DataSource getDataSource() {
            return DataSource.LOCAL;
        }
    }
}
