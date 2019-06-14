/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.text.TextUtils;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;

import org.mozilla.focus.R;

/**
 * A helper class to encapsulate actions for choosing file.
 * Assuming permissions is granted.
 */
public class FileChooseAction {

    public static final int REQUEST_CODE_CHOOSE_FILE = 103;

    private Fragment hostFragment;
    private ValueCallback<Uri[]> callback;
    private WebChromeClient.FileChooserParams params;
    private Uri[] uris;

    public FileChooseAction(@NonNull Fragment hostFragment,
                            @NonNull ValueCallback<Uri[]> callback,
                            @NonNull WebChromeClient.FileChooserParams params) {
        this.hostFragment = hostFragment;
        this.callback = callback;
        this.params = params;
    }

    public void cancel() {
        this.callback.onReceiveValue(null);
    }

    /**
     * Callback when back from a File-choose-activity
     *
     * @param resultCode
     * @param data
     * @return true if this action is done
     */
    public boolean onFileChose(int resultCode, Intent data) {
        if (this.callback == null) {
            return true;
        }

        if ((resultCode != Activity.RESULT_OK) || (data == null)) {
            this.callback.onReceiveValue(null);
            this.callback = null;
            return true;
        }

        try {
            final Uri uri = data.getData();
            uris = (uri == null) ? null : new Uri[]{uri};

            // FIXME: check permission before access the uri
            // if file locates on external storage and we haven't granted permission
            // we might get exception here. but try won't work here.
            this.callback.onReceiveValue(uris);
        } catch (Exception e) {
            this.callback.onReceiveValue(null);
            e.printStackTrace();
        }

        this.callback = null;
        return true;
    }

    public void startChooserActivity() {
        hostFragment.startActivityForResult(createChooserIntent(this.params), REQUEST_CODE_CHOOSE_FILE);
    }

    private Intent createChooserIntent(WebChromeClient.FileChooserParams params) {
        final String[] mimeTypes = params.getAcceptTypes();
        CharSequence title = params.getTitle();
        title = TextUtils.isEmpty(title) ? hostFragment.getString(R.string.file_picker_title) : title;

        return FilePickerUtil.getFilePickerIntent(hostFragment.getActivity(), title, mimeTypes);
    }
}
