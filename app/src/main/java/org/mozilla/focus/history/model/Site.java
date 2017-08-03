package org.mozilla.focus.history.model;

import android.graphics.Bitmap;

/**
 * Created by hart on 03/08/2017.
 */

public class Site {
    private long id;
    private String title;
    private String url;
    private long viweCount;
    private long lastViewTimestamp;
    private Bitmap favIcon;

    public long getId() {
        return this.id;
    }

    public String getTitle() {
        return this.title;
    }

    public String getUrl() {
        return this.url;
    }

    public long getViweCount() {
        return this.viweCount;
    }

    public long getLastViewTimestamp() {
        return this.lastViewTimestamp;
    }

    public Bitmap getFavIcon() {
        return this.favIcon;
    }
}
