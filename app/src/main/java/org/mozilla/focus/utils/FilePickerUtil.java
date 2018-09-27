/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FilePickerUtil {

    /* Gets an intent that can open a particular mimetype. Will show a prompt with a list
     * of Activities that can handle the mietype. Asynchronously calls the handler when
     * one of the intents is selected. If the caller passes in null for the handler, will still
     * prompt for the activity, but will throw away the result.
     */
    public static Intent getFilePickerIntent(
            final Context context,
            final CharSequence title,
            final String[] mimeTypes) {
        final List<Intent> intents = getIntentsForFilePicker(context, mimeTypes);

        if (intents.size() == 0) {
            return null;
        }

        final Intent base = intents.remove(0);

        if (intents.size() == 0) {
            return base;
        }

        final Intent chooser = Intent.createChooser(base, title);
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS,
                intents.toArray(new Parcelable[intents.size()]));
        return chooser;
    }

    public static List<Intent> getIntentsForFilePicker(final Context context,
                                                       final String[] mimeTypes) {

        // The base intent to use for the file picker. Even if this is an implicit intent, Android will
        // still show a list of Activities that match this action/type.
        Intent baseIntent;

        // A HashMap of Activities the base intent will show in the chooser. This is used
        // to filter activities from other intents so that we don't show duplicates.
        HashMap<String, Intent> baseIntents = new HashMap<String, Intent>();

        // A list of other activities to show in the picker (and the intents to launch them).
        HashMap<String, Intent> intents = new HashMap<String, Intent>();

        String mimeType = (mimeTypes == null) ? "*/*" : mimeTypes[0];
        if (("audio/*".equals(mimeType))
                || ("image/*".equals(mimeType))
                || ("video/*".equals(mimeType))) {
            // recognized mime type, do nothing
        } else {
            // fallback
            mimeType = "*/*";
        }

        addActivities(context, createIntent(mimeType), intents, baseIntents);

        // If we didn't find any activities, we fall back to the */* mimetype intent
        if (baseIntents.size() == 0 && intents.size() == 0) {
            intents.clear();

            baseIntent = createIntent("*/*");
            addActivities(context, baseIntent, baseIntents, null);
        }

        return new ArrayList<>(intents.values());
    }

    /**
     * For activities which could perform the intent, put them into *intents*.
     *
     * @param context
     * @param targetIntent to find out how many activities could perform this intent
     * @param intents      to put results in this map
     * @param filters      to filter out duplicate activities
     */
    private static void addActivities(@NonNull Context context,
                                      @NonNull Intent targetIntent,
                                      @NonNull HashMap<String, Intent> intents,
                                      @Nullable HashMap<String, Intent> filters) {

        if (filters == null) {
            return;
        }

        final PackageManager pm = context.getPackageManager();
        final List<ResolveInfo> lri = pm.queryIntentActivities(targetIntent, 0);

        for (ResolveInfo ri : lri) {
            final ComponentName cn = new ComponentName(ri.activityInfo.applicationInfo.packageName,
                    ri.activityInfo.name);
            if (!filters.containsKey(cn.toString())) {
                final Intent intent = new Intent(targetIntent);
                intent.setComponent(cn);
                intents.put(cn.toString(), intent);
            }
        }
    }

    private static Intent createIntent(String mimeType) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(mimeType);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        return intent;
    }
}
