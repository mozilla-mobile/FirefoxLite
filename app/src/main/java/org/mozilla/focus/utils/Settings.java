/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import org.mozilla.focus.R;
import org.mozilla.focus.fragment.FirstrunFragment;
import org.mozilla.focus.search.SearchEngine;

/**
 * A simple wrapper for SharedPreferences that makes reading preference a little bit easier.
 */
public class Settings {
    private static Settings instance;

    public synchronized static Settings getInstance(Context context) {
        if (instance == null) {
            instance = new Settings(context.getApplicationContext());
        }
        return instance;
    }

    private final SharedPreferences preferences;
    private final Resources resources;

    private Settings(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        resources = context.getResources();
    }

    public boolean shouldBlockImages() {
        boolean flag = preferences.getBoolean(
                resources.getString(R.string.pref_key_performance_block_images),
                true);
        return flag;
    }

    public boolean shouldShowFirstrun() {
        return !preferences.getBoolean(FirstrunFragment.FIRSTRUN_PREF, false);
    }

    public boolean shouldSaveToRemovableStorage() {
        final String key = getPreferenceKey(R.string.pref_key_privacy_storage_save_downloads_to);
        final String value = preferences.getString(key, "");

        // FIXME: rely on String-array-order is not a good idea
        final String[] defined = resources.getStringArray(R.array.data_saving_path_values);
        return defined[0].equals(value); // assume the first item is for removable storage
    }

    @Nullable
    public String getDefaultSearchEngineName() {
        return preferences.getString(getPreferenceKey(R.string.pref_key_search_engine), null);
    }

    public void setDefaultSearchEngine(SearchEngine searchEngine) {
        preferences.edit()
                .putString(getPreferenceKey(R.string.pref_key_search_engine), searchEngine.getName())
                .apply();
    }

    /* package */ String getPreferenceKey(int resourceId) {
        return resources.getString(resourceId);
    }
}
