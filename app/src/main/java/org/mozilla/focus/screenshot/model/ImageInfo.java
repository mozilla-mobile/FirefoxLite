package org.mozilla.focus.screenshot.model;

public class ImageInfo {
    public String title;

    public ImageInfo(String title) {
        this.title = title;
    }

    @Override
    public String toString() {
        return "ImageInfo{" +
                "title='" + title + '\'' +
                '}';
    }
}
