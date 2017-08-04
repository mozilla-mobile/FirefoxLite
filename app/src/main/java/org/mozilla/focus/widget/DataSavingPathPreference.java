/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget;

import android.content.Context;
import android.preference.ListPreference;
import android.text.TextUtils;
import android.util.AttributeSet;

import org.mozilla.focus.R;

public class DataSavingPathPreference extends ListPreference {
    private static final String LOG_TAG = "DataSavingPathPreference";

    public DataSavingPathPreference(Context context) {
        this(context, null);
    }

    public DataSavingPathPreference(Context context, AttributeSet attributes) {
        super(context, attributes);

    }

    @Override
    protected void onAttachedToActivity() {
        super.onAttachedToActivity();

        buildList();
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        // The superclass will take care of persistence.
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            persistString(getValue());
        }

    }

    @Override
    public CharSequence getSummary() {
        if (TextUtils.isEmpty(getValue())) {
            final String[] values = getContext().getResources().getStringArray(R.array.data_saving_path_values);
            setValueIndex(0);
            return values[0];
        }

        return getValue();
    }

    private void buildList() {
        final String[] entries = getContext().getResources().getStringArray(R.array.data_saving_path_entries);
        final String[] values = getContext().getResources().getStringArray(R.array.data_saving_path_values);

        setEntries(entries);
        setEntryValues(values);
    }
}
