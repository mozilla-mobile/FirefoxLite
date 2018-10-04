/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.screenshot.model;

import android.support.annotation.WorkerThread;

import java.io.Serializable;

/**
 * Created by hart on 15/08/2017.
 */

public class Screenshot implements Serializable {
    private long id;
    private String title;
    private String url;
    private long timestamp;
    private String imageUri;
    private String category = "";
    private int categoryVersion = 0;

    public Screenshot() {
    }

    public Screenshot(String title, String url, long timestamp, String imageUri) {
        this.title = title;
        this.url = url;
        this.timestamp = timestamp;
        this.imageUri = imageUri;
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return this.url;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getImageUri() {
        return this.imageUri;
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }

    // category is only set in a background thread since it's loading should be async.
    @WorkerThread
    public void setCategory(String category) {
        this.category = category;
    }

    public String getCategory() {
        return category;
    }

    public int getCategoryVersion() {
        return categoryVersion;
    }

    public void setCategoryVersion(int categoryVersion) {
        this.categoryVersion = categoryVersion;
    }

    @Override
    public String toString() {
        return "Screenshot{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", url='" + url + '\'' +
                ", timestamp=" + timestamp +
                ", imageUri='" + imageUri + '\'' +
                '}';
    }
}
