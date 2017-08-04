/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.web;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

public class Download implements Parcelable {

    public static final int TYPE_OTHER = 0;
    public static final int TYPE_IMAGE = 1;

    public static final Parcelable.Creator<Download> CREATOR = new Parcelable.Creator<Download>() {

        @Override
        public Download createFromParcel(Parcel source) {
            return new Download(
                    source.readString(),
                    source.readString(),
                    source.readString(),
                    source.readString(),
                    source.readLong(),
                    source.readInt());
        }

        @Override
        public Download[] newArray(int size) {
            return new Download[size];
        }
    };

    private final String url;
    private final String contentDisposition;
    private final String mimeType;
    private final long contentLength;
    private final String userAgent;
    private final int downloadtype;

    public Download(@NonNull String url,
                    @NonNull String userAgent,
                    @NonNull String contentDisposition,
                    @NonNull String mimeType,
                    long contentLength,
                    int type) {
        this.url = url;
        this.userAgent = userAgent;
        this.contentDisposition = contentDisposition;
        this.mimeType = mimeType;
        this.contentLength = contentLength;
        this.downloadtype = type;
    }

    public String getUrl() {
        return url;
    }

    public String getContentDisposition() {
        return contentDisposition;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getContentLength() {
        return contentLength;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public int getDownloadType() {
        return downloadtype;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(url);
        dest.writeString(userAgent);
        dest.writeString(contentDisposition);
        dest.writeString(mimeType);
        dest.writeLong(contentLength);
    }
}
