package org.mozilla.focus.persistence;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;
import android.text.TextUtils;

@Entity(tableName = "tabs")
public class TabEntity {

    @Ignore
    public TabEntity(String id, String parentId) {
        this(id, parentId, "", "");
    }

    public TabEntity(String id, String parentId, String title, String url) {
        this.id = id;
        this.parentId = parentId;
        this.title = title;
        this.url = url;
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

    public boolean isValid() {
        final boolean hasId = !TextUtils.isEmpty(this.getId());
        final boolean hasUrl = !TextUtils.isEmpty(this.getUrl());

        return hasId && hasUrl;
    }

    @Override
    public String toString() {
        return "TabEntity{" +
                "id='" + id + '\'' +
                ", parentId='" + parentId + '\'' +
                ", title='" + title + '\'' +
                ", url='" + url +
                '}';
    }
}
