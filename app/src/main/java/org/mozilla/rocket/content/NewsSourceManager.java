/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.content;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.mozilla.focus.utils.Settings;
import org.mozilla.threadutils.ThreadUtils;

import static org.mozilla.rocket.widget.NewsSourcePreference.NEWS_DB;
import static org.mozilla.rocket.widget.NewsSourcePreference.NEWS_NP;
import static org.mozilla.rocket.widget.NewsSourcePreference.PREF_INT_NEWS_PRIORITY;

public class NewsSourceManager {
    public static final String TAG = "NewsSource";

    private static NewsSourceManager instance = new NewsSourceManager();

    private String newsSource = null;

    private String newsSourceUrl = "";

    public static NewsSourceManager getInstance() {
        return instance;
    }

    private NewsSourceManager() {
    }

    public void init(Context context) {

        ThreadUtils.postToBackgroundThread(() -> {
            final Settings settings = Settings.getInstance(context);
            final String source = settings.getNewsSource();
            // "DainikBhaskar.com" doesn't provide their feed anymore. Switch to "Newspoint" by default
            if (TextUtils.isEmpty(source) || NEWS_DB.equals(source)) {
                newsSource = NEWS_NP;
                Log.d(NewsSourceManager.TAG, "NewsSourceManager sets default:" + newsSource);

                settings.setNewsSource(newsSource);
                settings.setPriority(PREF_INT_NEWS_PRIORITY, Settings.PRIORITY_SYSTEM);
            } else {
                newsSource = settings.getNewsSource();
                Log.d(NewsSourceManager.TAG, "NewsSourceManager already set:" + newsSource);
            }
        });
    }

    public String getNewsSource() {
        if (newsSource == null) {
            throw new IllegalStateException("NewsSourceManager is not initialized");
        }
        return newsSource;
    }

    public void setNewsSource(String newsSource) {
        this.newsSource = newsSource;
        NewsRepository.reset();
    }

    public String getNewsSourceUrl() {
        return newsSourceUrl;
    }

    public void setNewsSourceUrl(String newsSourceUrl) {
        this.newsSourceUrl = newsSourceUrl;
        NewsRepository.resetSubscriptionUrl(newsSourceUrl);
    }
}
