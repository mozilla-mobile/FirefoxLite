/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.awesomebar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import mozilla.components.concept.awesomebar.AwesomeBar
import org.mozilla.focus.history.model.Site
import org.mozilla.focus.repository.BookmarkRepository
import org.mozilla.focus.utils.DimenUtils
import org.mozilla.icon.FavIconUtils
import org.mozilla.rocket.persistance.History.HistoryRepository
import org.mozilla.rocket.tabs.SessionManager
import java.util.Locale
import java.util.UUID

private class AwesomeBarSite(
    var site: Site,
    var weight: Double,
    var tabId: String = "",
    var isBookmark: Boolean = false
) : Comparable<AwesomeBarSite> {
    override fun compareTo(other: AwesomeBarSite): Int {
        return if (this.weight > other.weight) {
            1
        } else {
            0
        }
    }
}

/**
 * Mix History, Bookmark, Tabs all together with a simple algorithm.
 *
 * */
class FrecensySuggestionProvider(
    private val context: Context,
    private val switchToTabIcon: Bitmap?,
    private val bookmarkRepository: BookmarkRepository,
    private val historyRepository: HistoryRepository,
    private val sessionManager: SessionManager,
    private val onSwitchTabAction: ((sessionManager: SessionManager, text: String) -> Unit),
    private val onBookmarkAction: ((text: String) -> Unit),
    private val onHistoryAction: ((text: String) -> Unit)

) : AwesomeBar.SuggestionProvider {
    override val id: String = UUID.randomUUID().toString()

    companion object {
        private const val DEFAULT_SITE_ID = -1L
        private const val DEFAULT_SITE_VIEW_COUNT = 100L
        private const val DEFAULT_SITE_WEIGHT = 0.0

        private const val SUGGESTION_MULTIPLIER = 1.75
        private const val SUGGESTION_QUERY_LIMIT = 100
        private const val DAY_IN_MS = 24 * 60 * 60 * 1000
        private const val FRECENCY_DAY_5 = 5.0 * DAY_IN_MS
        private const val FRECENCY_DAY_15 = 15.0 * DAY_IN_MS
        private const val FRECENCY_DAY_32 = 32.0 * DAY_IN_MS
        private const val FRECENCY_DAY_91 = 91.0 * DAY_IN_MS

        private const val FRECENCY_WEIGHT_5 = 100.0
        private const val FRECENCY_WEIGHT_15 = 70.0
        private const val FRECENCY_WEIGHT_32 = 50.0
        private const val FRECENCY_WEIGHT_91 = 30.0
        private const val FRECENCY_WEIGHT_OTHER = 10.0
    }

    override suspend fun onInputChanged(text: String): List<AwesomeBar.Suggestion> {

        if (text.isEmpty()) {
            return emptyList()
        }
        val history = historyRepository.searchHistory("%$text%", SUGGESTION_QUERY_LIMIT)
        val bookmarks = bookmarkRepository.searchBookmarks("%$text%", SUGGESTION_QUERY_LIMIT)
        val tabs = sessionManager.getTabs()
        val now = System.currentTimeMillis()
        val candidate = HashMap<String, AwesomeBarSite>()
        var idSeq = DEFAULT_SITE_ID

        // add all history entries to candidate
        history.forEach {
            val diff = now - it.lastViewTimestamp
            val weight = when {
                diff < FRECENCY_DAY_5 -> FRECENCY_WEIGHT_5
                diff < FRECENCY_DAY_15 -> FRECENCY_WEIGHT_15
                diff < FRECENCY_DAY_32 -> FRECENCY_WEIGHT_32
                diff < FRECENCY_DAY_91 -> FRECENCY_WEIGHT_91
                else -> FRECENCY_WEIGHT_OTHER
            }
            candidate[it.url] = AwesomeBarSite(site = it, weight = weight * it.viewCount)
        }

        // add all bookmark entries to candidate
        // if it's in the history, the weight times 1.75
        // if it's not in history, give it the weight 10K (100  * 100 view count)
        bookmarks.forEach {
            if (candidate[it.url] == null) {
                candidate[it.url] =
                    AwesomeBarSite(
                        site = Site(idSeq--, it.title, it.url, DEFAULT_SITE_VIEW_COUNT, now, ""),
                        weight = FRECENCY_WEIGHT_5 * DEFAULT_SITE_VIEW_COUNT,
                        isBookmark = true
                    )
            } else {
                candidate[it.url]?.weight =
                    candidate[it.url]?.weight?.times(SUGGESTION_MULTIPLIER) ?: DEFAULT_SITE_WEIGHT
            }
        }

        // add all tab entries to candidate
        // if it's in the history+bookmark, the weight times 1.75
        // if it's not in history+bookmark, give it the weight 10K (100  * 100 view count)
        tabs.forEach {
            val url = it.url
            val title = it.title
            if (url == null || !url.contains(text) && !title.toLowerCase(Locale.getDefault())
                    .contains(text)
            ) {
                return@forEach
            }
            if (candidate[url] == null) {
                candidate[url] = AwesomeBarSite(
                    site = Site(idSeq--, it.title, url, DEFAULT_SITE_VIEW_COUNT, now, ""),
                    weight = FRECENCY_WEIGHT_5 * DEFAULT_SITE_VIEW_COUNT,
                    tabId = it.id
                )
            } else {
                candidate[url]?.weight =
                    candidate[url]?.weight?.times(SUGGESTION_MULTIPLIER) ?: DEFAULT_SITE_WEIGHT
                candidate[url]?.tabId = it.id
            }
        }

        return candidate.values
            .sortedByDescending { it.weight }
            .map { awesomeBarSite ->
                makeAwesomeBarSuggestion(awesomeBarSite)
            }
    }

    private fun makeAwesomeBarSuggestion(
        awesomeBarSite: AwesomeBarSite
    ): AwesomeBar.Suggestion {

        val site = awesomeBarSite.site
        val tabId = awesomeBarSite.tabId
        val url = site.url
        val title = site.title ?: ""
        val siteId = site.id.toString()

        return AwesomeBar.Suggestion(
            provider = this,
            id = siteId,
            title = title,
            description = url,

            icon = if (tabId.isNotEmpty()) {
                { _, _ -> switchToTabIcon }
            } else {
                { _, _ -> chooseTheRightBitmap(awesomeBarSite) }
            },

            onSuggestionClicked = when {
                tabId.isNotEmpty() -> {
                    { onSwitchTabAction(sessionManager, tabId) }
                }
                awesomeBarSite.isBookmark -> {
                    { onBookmarkAction(url) }
                }
                else -> {
                    { onHistoryAction(url) }
                }
            }
        )
    }

    private fun chooseTheRightBitmap(awesomeBarSite: AwesomeBarSite): Bitmap? {
        val favIconUri: String = awesomeBarSite.site.favIconUri ?: ""

        val bitmapFromUri: Bitmap? = FavIconUtils.getBitmapFromUri(context, favIconUri)

        return if (bitmapFromUri == null || DimenUtils.iconTooBlurry(
                context.resources,
                bitmapFromUri.width
            )
        ) {
            val backgroundColor: Int =
                if (bitmapFromUri == null) Color.WHITE else FavIconUtils.getDominantColor(
                    bitmapFromUri
                )
            val url: String = awesomeBarSite.site.url
            DimenUtils.getInitialBitmap(
                context.resources,
                FavIconUtils.getRepresentativeCharacter(url),
                backgroundColor
            )
        } else {
            bitmapFromUri
        }
    }
}
