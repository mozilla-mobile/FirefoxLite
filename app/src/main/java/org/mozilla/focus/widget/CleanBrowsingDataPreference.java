/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.preference.MultiSelectListPreference;
import android.util.AttributeSet;
import android.webkit.CookieManager;
import android.webkit.WebViewDatabase;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.fileutils.FileUtils;
import org.mozilla.focus.R;
import org.mozilla.focus.history.BrowsingHistoryManager;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.utils.TopSitesUtils;
import org.mozilla.rocket.component.PrivateSessionNotificationService;
import org.mozilla.rocket.privately.PrivateMode;

import java.util.Set;

/**
 * Created by ylai on 2017/8/3.
 */

public class CleanBrowsingDataPreference extends MultiSelectListPreference {

    public CleanBrowsingDataPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CleanBrowsingDataPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        builder.setTitle(null);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            Resources resources = getContext().getResources();
            //  On click positive callback here get current value by getValues();
            for (String value : getValues()) {
                if (resources.getString(R.string.pref_value_clear_browsing_history).equals(value)) {
                    BrowsingHistoryManager.getInstance().deleteAll(null);
                    TopSitesUtils.getDefaultSitesJsonArrayFromAssets(getContext());
                } else if (resources.getString(R.string.pref_value_clear_cookies).equals(value)) {
                    CookieManager.getInstance().removeAllCookies(null);
                    // Also clear cookies in private mode process if the process exist
                    if (PrivateMode.isPrivateModeProcessRunning(getContext())) {
                        // If there's a private mode process running, below intent will reach
                        // PrivateModeActivity's onNewIntent, thus the activity won't appear again.
                        // (assume that onNewIntent will always runs before onStart()
                        // Fixme: we should rely on another Android component for IPC to clear CookieManager
                        // Fixme: rather than rely on PrivateModeActivity cause it will cause UI issue easily
                        final Intent intent = PrivateSessionNotificationService.
                                buildIntent(getContext().getApplicationContext(), true);
                        getContext().startActivity(intent);
                    }
                } else if (resources.getString(R.string.pref_value_clear_cache).equals(value)) {
                    FileUtils.clearCache(getContext());
                } else if (resources.getString(R.string.pref_value_clear_form_history).equals(value)) {
                    WebViewDatabase.getInstance(getContext()).clearFormData();
                }
                TelemetryWrapper.settingsEvent(getKey(), value);
            }

            if (getValues().size() > 0) {
                Toast.makeText(getContext(), R.string.message_cleared_browsing_data, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Object flattenToJsonObject(Set<String> values) {
        final JSONObject object = new JSONObject();

        final String[] preferenceKeys = getContext().getResources().getStringArray(R.array.clean_browsing_data_values);
        if (preferenceKeys.length <= 0) {
            return object;
        }

        for (String key : preferenceKeys) {
            try {
                if (values.contains(key)) {
                    object.put(key, Boolean.TRUE.toString());
                } else {
                    object.put(key, JSONObject.NULL);
                }
            } catch (JSONException e) {
                throw new AssertionError("Preference value can't be serialized to JSON", e);
            }
        }

        return object;
    }

}
