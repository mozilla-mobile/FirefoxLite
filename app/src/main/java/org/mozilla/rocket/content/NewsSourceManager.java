/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.content;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.mozilla.focus.utils.Settings;
import org.mozilla.threadutils.ThreadUtils;

import java.util.Random;

import static org.mozilla.rocket.widget.NewsSourcePreference.*;

public class NewsSourceManager {
    public static final String TAG = "NewsSource";

    private static NewsSourceManager instance = new NewsSourceManager();

    private String newsSource = null;

    private boolean loadHasBeenTriggered;

    public static NewsSourceManager getInstance() {
        return instance;
    }

    private NewsSourceManager() {
    }

    public void init(Context context) {

        loadHasBeenTriggered = true;
        ThreadUtils.postToBackgroundThread(() -> {
            final Settings settings = Settings.getInstance(context);
            final String source = settings.getNewsSource();
            if (TextUtils.isEmpty(source)) {
                if (new Random().nextInt(2) % 2 == 0) {
                    newsSource = NEWS_DB;
                } else {
                    newsSource = NEWS_NP;
                }
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
        awaitLoadingSearchEnginesLocked();
        return newsSource;
    }

    public void setNewsSource(String newsSource) {
        this.newsSource = newsSource;
        ContentRepository.reset();
    }

    // Our (searchEngines == null) check is deemed to be an unsynchronised access. Similarly loadHasBeenTriggered
    // also doesn't need synchronisation:
    @SuppressFBWarnings(value = "IS2_INCONSISTENT_SYNC", justification = "Variable is not being accessed, it is merely being tested for existence")
    public void awaitLoadingSearchEnginesLocked() {
        if (!loadHasBeenTriggered) {
            throw new IllegalStateException("Attempting to retrieve search engines without a corresponding init()");
        }

        while (newsSource == null) {
            try {
                wait();
            } catch (InterruptedException ignored) {
                // Ignore
            }
        }
    }
}
