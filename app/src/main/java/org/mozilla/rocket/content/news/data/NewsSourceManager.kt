/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.content.news.data

import android.content.Context
import android.text.TextUtils
import android.util.Log

import org.mozilla.focus.utils.AppConfigWrapper
import org.mozilla.focus.utils.Settings
import org.mozilla.threadutils.ThreadUtils

class NewsSourceManager private constructor() {

    private var newsSource: String? = null

    var newsSourceUrl = ""

    fun init(context: Context) {

        ThreadUtils.postToBackgroundThread {
            val settings = Settings.getInstance(context)
            val source = settings.newsSource
            if (TextUtils.isEmpty(source) || !NEWS_SOURCE_LIST.contains(source)) {
                newsSource = NEWS_NP
                Log.d(TAG, "NewsSourceManager sets default:$newsSource")

                settings.newsSource = newsSource
                settings.setPriority(PREF_INT_NEWS_PRIORITY, Settings.PRIORITY_SYSTEM)
            } else {
                newsSource = settings.newsSource
                Log.d(TAG, "NewsSourceManager already set:$newsSource")
            }
            // previously we only set the source url after remote config is fetched. But when there's no internet connection,
            // the remote config's callback will never be called, thus newsSourceUrl will always be null.
            // We try to set the url here if previously we already got the value from remote config.
            val url = AppConfigWrapper.getNewsProviderUrl(settings.newsSource)
            if (!TextUtils.isEmpty(url)) {
                this.newsSourceUrl = url
            }
        }
    }

    fun setNewsSource(newsSource: String) {
        this.newsSource = newsSource
    }

    companion object {
        const val TAG = "NewsSource"
        const val PREF_INT_NEWS_PRIORITY = "pref_int_news_priority"

        @JvmStatic
        val instance = NewsSourceManager()

        private const val NEWS_NP = "Newspoint"
        private val NEWS_SOURCE_LIST = listOf(NEWS_NP)
    }
}
