/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import org.mozilla.focus.utils.AppConstants;
import org.mozilla.focus.utils.SafeIntent;
import org.mozilla.focus.utils.SearchUtils;
import org.mozilla.rocket.component.LaunchIntentDispatcher;

/**
 * Activity for receiving and processing an ACTION_PROCESS_TEXT intent.
 */
public class TextActionActivity extends Activity {
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final SafeIntent intent = new SafeIntent(getIntent());

        final CharSequence searchTextCharSequence = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
        final String searchText;
        if (searchTextCharSequence != null) {
            searchText = searchTextCharSequence.toString();
        } else {
            searchText = "";
        }
        final String searchUrl = SearchUtils.createSearchUrl(this, searchText);

        final Intent searchIntent = new Intent();
        searchIntent.setClassName(this, AppConstants.LAUNCHER_ACTIVITY_ALIAS);
        searchIntent.setAction(Intent.ACTION_VIEW);
        searchIntent.putExtra(LaunchIntentDispatcher.LaunchMethod.EXTRA_BOOL_TEXT_SELECTION.getValue(), true);
        searchIntent.setData(Uri.parse(searchUrl));

        startActivity(searchIntent);

        finish();
    }
}
