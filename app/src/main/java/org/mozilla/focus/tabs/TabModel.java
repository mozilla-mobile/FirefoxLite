/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.tabs;

import android.graphics.Bitmap;
import android.os.Bundle;

/**
 * A model class to describe a tab, and these fields are supposed to be stored in persistent storage
 * for restoring tabs if app is killed.
 */
public class TabModel {

    /* package */ String title;

    /* package */ String url;

    /**
     * ViewState for this Tab. Usually to fill by WebView.saveViewState(Bundle)
     */
    /* package */ Bundle webViewState;

    /**
     * Thumbnail for this tab for previewing.
     */
    /* package */ Bitmap thumbnail;
}
