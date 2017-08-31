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

    public static final int STORAGE_MSG_TYPE_REMOVABLE_UNAVAILABLE = 0x9527; // beautiful random number
    public static final int STORAGE_MSG_TYPE_REMOVABLE_AVAILABLE = 0x5987;

    private static Settings instance;
    private static final boolean BLOCK_IMAGE_DEFAULT = false;
    private static final boolean TURBO_MODE_DEFAULT = true;
    private static final boolean DID_SHOW_RATE_APP_DEFAULT = false;

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
        return preferences.getBoolean(
                resources.getString(R.string.pref_key_performance_block_images),
                BLOCK_IMAGE_DEFAULT);
    }

    public void setBlockImages(boolean blockImages) {
        final String key = getPreferenceKey(R.string.pref_key_performance_block_images);
        preferences.edit().putBoolean(key, blockImages).apply();
    }

    public boolean shouldShowFirstrun() {
        return !preferences.getBoolean(FirstrunFragment.FIRSTRUN_PREF, false);
    }

    public boolean shouldSaveToRemovableStorage() {
        // FIXME: rely on String-array-order is not a good idea
        final String[] defined = resources.getStringArray(R.array.data_saving_path_values);

        final String key = getPreferenceKey(R.string.pref_key_storage_save_downloads_to);
        final String value = preferences.getString(key, defined[0]);

        return defined[0].equals(value); // assume the first item is for removable storage
    }

    public boolean shouldUseTurboMode(){
        return preferences.getBoolean(
                resources.getString(R.string.pref_key_turbo_mode),
                TURBO_MODE_DEFAULT);
    }

    public void setTurboMode(boolean toEnable) {
        final String key = getPreferenceKey(R.string.pref_key_turbo_mode);
        preferences.edit().putBoolean(key, toEnable).apply();
    }


    public void setRemovableStorageStateOnCreate(boolean exist) {
        final String key = getPreferenceKey(R.string.pref_key_removable_storage_available_on_create);
        preferences.edit().putBoolean(key, exist).apply();
    }

    public boolean getRemovableStorageStateOnCreate() {
        final String key = getPreferenceKey(R.string.pref_key_removable_storage_available_on_create);
        return preferences.getBoolean(key, false);
    }

    public int getShowedStorageMessage() {
        final String key = getPreferenceKey(R.string.pref_key_showed_storage_message);
        return preferences.getInt(key, STORAGE_MSG_TYPE_REMOVABLE_UNAVAILABLE);
    }

    public void setShowedStorageMessage(final int type) {
        if (type != STORAGE_MSG_TYPE_REMOVABLE_AVAILABLE
                && type != STORAGE_MSG_TYPE_REMOVABLE_UNAVAILABLE) {
            throw new RuntimeException("Unknown message type");
        }

        final String key = getPreferenceKey(R.string.pref_key_showed_storage_message);
        preferences.edit()
                .putInt(key, type)
                .apply();
    }

    @Nullable
    public String getDefaultSearchEngineName() {
        return preferences.getString(getPreferenceKey(R.string.pref_key_search_engine), null);
    }

    public boolean didShowRateAppDialog() {
        return preferences.getBoolean(getPreferenceKey(R.string.pref_key_did_show_app_rate_dialog), DID_SHOW_RATE_APP_DEFAULT);
    }

    public void setRateAppDialogDidShow() {
        preferences.edit()
                .putBoolean(getPreferenceKey(R.string.pref_key_did_show_app_rate_dialog), true)
                .apply();
    }

    public void increaseAppCreateCounter() {
        int count = getAppCreateCount();
        preferences.edit()
                .putInt(getPreferenceKey(R.string.pref_key_app_create_counter), ++count)
                .apply();
    }

    public int getAppCreateCount() {
        return preferences.getInt(getPreferenceKey(R.string.pref_key_app_create_counter), 0);
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
