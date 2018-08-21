/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.history.model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;

@Entity(tableName = "browsing_history")
public class Site {

    // TODO: 8/21/18 For compatibility to old SQLiteOpenHelper implementation only, should be removed.
    @Ignore
    public Site() {

    }

    public Site(long id, String title, @NonNull String url, int viewCount, int lastViewTimestamp, String favIconUri) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.viewCount = viewCount;
        this.lastViewTimestamp = lastViewTimestamp;
        this.favIconUri = favIconUri;
    }

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    private long id;

    private String title;

    @NonNull
    private String url;

    @ColumnInfo(name = "view_count")
    private long viewCount;

    @ColumnInfo(name = "last_view_timestamp")
    private long lastViewTimestamp;

    @ColumnInfo(name = "fav_icon_uri")
    private String favIconUri;

    // TODO: 8/21/18 Deprecate and remove this
    @Ignore
    private Bitmap favIcon;

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

    @NonNull
    public String getUrl() {
        return this.url;
    }

    public void setUrl(@NonNull String url) {
        this.url = url;
    }

    public long getViewCount() {
        return this.viewCount;
    }

    public void setViewCount(long count) {
        this.viewCount = count;
    }

    public long getLastViewTimestamp() {
        return this.lastViewTimestamp;
    }

    public void setLastViewTimestamp(long timestamp) {
        this.lastViewTimestamp = timestamp;
    }

    public Bitmap getFavIcon() {
        return this.favIcon;
    }

    public void setFavIcon(Bitmap favIcon) {
        this.favIcon = favIcon;
    }

    public String getFavIconUri() {
        return favIconUri;
    }

    public void setFavIconUri(String favIconUri) {
        this.favIconUri = favIconUri;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Site) {
            if (((Site) obj).getId() == this.getId()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return "HistoryModel{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", url='" + url + '\'' +
                ", viewCount='" + viewCount + '\'' +
                ", lastViewTimestamp='" + lastViewTimestamp + '\'' +
                ", favIconUri='" + favIconUri + '\'' +
                '}';
    }
}
