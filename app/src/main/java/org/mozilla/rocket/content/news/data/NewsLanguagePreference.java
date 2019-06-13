/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.content.news.data;

import android.content.Context;
import android.support.v7.preference.ListPreference;
import android.text.TextUtils;
import android.util.AttributeSet;

import java.util.HashMap;
import java.util.Map;

public class NewsLanguagePreference extends ListPreference {
    private static final String LOG_TAG = "GeckoLocaleList";

    private static final Map<String, String> languageCodeToNameMap = new HashMap<>();

    static {
        // Only ICU 57 actually contains the Asturian name for Asturian, even Android 7.1 is still
        // shipping with ICU 56, so we need to override the Asturian name (otherwise displayName will
        // be the current locales version of Asturian, see:
        // https://github.com/mozilla-mobile/focus-android/issues/634#issuecomment-303886118
        languageCodeToNameMap.put("english", "english");
        languageCodeToNameMap.put("ast", "Asturianu");
        // On an Android 8.0 device those languages are not known and we need to add the names
        // manually. Loading the resources at runtime works without problems though.
        languageCodeToNameMap.put("cak", "Kaqchikel");
        languageCodeToNameMap.put("ia", "Interlingua");
        languageCodeToNameMap.put("meh", "Tu´un savi ñuu Yasi'í Yuku Iti");
        languageCodeToNameMap.put("mix", "Tu'un savi");
        languageCodeToNameMap.put("trs", "Triqui");
        languageCodeToNameMap.put("zam", "DíɁztè");
        languageCodeToNameMap.put("oc", "occitan");
        languageCodeToNameMap.put("an", "Aragonés");
        languageCodeToNameMap.put("tt", "татарча");
        languageCodeToNameMap.put("wo", "Wolof");
        languageCodeToNameMap.put("anp", "अंगिका");
        languageCodeToNameMap.put("ixl", "Ixil");
        languageCodeToNameMap.put("pai", "Paa ipai");
        languageCodeToNameMap.put("quy", "Chanka Qhichwa");
        languageCodeToNameMap.put("ay", "Aimara");
        languageCodeToNameMap.put("quc", "K'iche'");
        languageCodeToNameMap.put("tsz", "P'urhepecha");
        languageCodeToNameMap.put("mai", "मैथिली/মৈথিলী");
        languageCodeToNameMap.put("jv", "Basa Jawa");
        languageCodeToNameMap.put("su", "Basa Sunda");
        languageCodeToNameMap.put("ace", "Basa Acèh");
        languageCodeToNameMap.put("gor", "Bahasa Hulontalo");
    }

    public NewsLanguagePreference(Context context) {
        this(context, null);
    }

    public NewsLanguagePreference(Context context, AttributeSet attributes) {
        super(context, attributes);
        buildList();

    }

    @Override
    public CharSequence getSummary() {
        final String value = getValue();

        if (TextUtils.isEmpty(value)) {
            return "english";
        }

        return languageCodeToNameMap.get(value);
    }

    private void buildList() {
        setEntries(languageCodeToNameMap.values().toArray(new String[]{}));
        setEntryValues(languageCodeToNameMap.keySet().toArray(new String[]{}));
    }
}
