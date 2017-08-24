package org.mozilla.focus.screenshot.model;

public class ImageInfo {
    public String title;
    public String data;

    public ImageInfo(String title, String data) {
        this.title = title;
        this.data = data;
    }

    @Override
    public String toString() {
        return "ImageInfo{" +
                "title='" + title + '\'' +
                ", data='" + data + '\'' +
                '}';
    }
}
