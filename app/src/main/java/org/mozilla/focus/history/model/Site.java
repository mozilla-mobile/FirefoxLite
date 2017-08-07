package org.mozilla.focus.history.model;

import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;

/**
 * Created by hart on 03/08/2017.
 */

public class Site {
    private long id;
    private String title;
    private String url;
    private long viewCount;
    private long lastViewTimestamp;
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

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getViweCount() {
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

    public byte[] getFavIconInBytes() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        this.favIcon.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }
}
