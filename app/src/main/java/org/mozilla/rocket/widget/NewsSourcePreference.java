/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.widget;

import android.content.Context;
import android.preference.ListPreference;
import android.support.annotation.StringDef;
import android.util.AttributeSet;
import android.util.Log;
import org.mozilla.focus.utils.AppConfigWrapper;
import org.mozilla.focus.utils.Settings;
import org.mozilla.rocket.content.NewsSourceManager;

public class NewsSourcePreference extends ListPreference {

    public final static String NEWS_DB = "DainikBhaskar.com";
    public final static String NEWS_NP = "Newspoint";

    @StringDef({NEWS_DB, NEWS_NP})
    public @interface NewsSource {
    }

    private static final String LOG_TAG = "NewsSourcePreference";

    public final static String PREF_INT_NEWS_PRIORITY = "pref_int_news_priority";

    public NewsSourcePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public NewsSourcePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public NewsSourcePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NewsSourcePreference(Context context) {
        super(context);
    }

    @Override
    protected void onAttachedToActivity() {
        super.onAttachedToActivity();

        final String[] entries = {NEWS_DB, NEWS_NP};


        setEntries(entries);
        setEntryValues(entries);

        setSummary(getValue());
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        // The superclass will take care of persistence.
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            persistString(getValue());
            // always accept users's input
            Settings.getInstance(getContext()).setPriority(PREF_INT_NEWS_PRIORITY, Settings.PRIORITY_USER);
            setSummary(getValue());
            NewsSourceManager.getInstance().setNewsSource(getValue());
            NewsSourceManager.getInstance().setNewsSourceUrl(AppConfigWrapper.getLifeFeedProviderUrl(getValue()));

            Log.d(NewsSourceManager.TAG, "User setup pref:" + getValue());
        }
    }
}
