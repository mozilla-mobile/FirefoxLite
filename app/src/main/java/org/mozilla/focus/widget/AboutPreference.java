/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

import org.mozilla.focus.R;

public class AboutPreference extends Preference {
    public AboutPreference(Context context, AttributeSet attrs,
                           int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        final String title = getContext().getResources().getString(R.string.preference_about);

        setTitle(title);
    }

    public AboutPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
}
