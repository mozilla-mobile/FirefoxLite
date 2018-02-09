package org.mozilla.focus.persistence;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;

@Entity(tableName = "tabs")
public class TabModel {

    @Ignore
    public TabModel(String id, String parentId) {
        this(id, parentId, "", "", "", "");
    }

    @Ignore
    public TabModel(String id, String parentId, String title, String url) {
        this(id, parentId, title, url, "", "");
    }

    public TabModel(String id, String parentId, String title, String url, String thumbnailUri, String webViewStateUri) {
        this.id = id;
        this.parentId = parentId;
        this.title = title;
        this.url = url;
        this.thumbnailUri = thumbnailUri;
        this.webViewStateUri = webViewStateUri;
    }

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "tab_id")
    private String id;

    @ColumnInfo(name = "tab_parent_id")
    private String parentId;

    @ColumnInfo(name = "tab_title")
    private String title;

    @ColumnInfo(name = "tab_url")
    private String url;

    /**
     * Thumbnail uri for tab preview.
     */
    @ColumnInfo(name = "tab_thumbnail_uri")
    private String thumbnailUri;

    @ColumnInfo(name = "webview_state_uri")
    private String webViewStateUri;

    /**
     * Thumbnail bitmap for tab previewing.
     */
    @Ignore
    private Bitmap thumbnail;

    /**
     * ViewState for this Tab. Usually to fill by WebView.saveViewState(Bundle)
     * Set it as @Ignore to avoid storing this field into database.
     * It will be serialized to a file and save the uri path into webViewStateUri field.
     */
    @Ignore
    private Bundle webViewState;

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getThumbnailUri() {
        return thumbnailUri;
    }

    public void setThumbnailUri(String thumbnailUri) {
        this.thumbnailUri = thumbnailUri;
    }

    public String getWebViewStateUri() {
        return webViewStateUri;
    }

    public void setWebViewStateUri(String webViewStateUri) {
        this.webViewStateUri = webViewStateUri;
    }

    public Bitmap getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(Bitmap thumbnail) {
        this.thumbnail = thumbnail;
    }

    public Bundle getWebViewState() {
        return webViewState;
    }

    public void setWebViewState(Bundle webViewState) {
        this.webViewState = webViewState;
    }

    @Override
    public String toString() {
        return "TabModel{" +
                "id='" + id + '\'' +
                ", parentId='" + parentId + '\'' +
                ", title='" + title + '\'' +
                ", url='" + url + '\'' +
                ", thumbnailUri='" + thumbnailUri + '\'' +
                ", webViewStateUri='" + webViewStateUri + '\'' +
                ", thumbnail=" + thumbnail +
                ", webViewState=" + webViewState +
                '}';
    }
}
