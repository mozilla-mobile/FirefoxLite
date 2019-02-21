/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.rocket.urlinput

import android.net.Uri

/**
 * The class holds the Quick Search setting.
 * The order of param is important cause we'll operate them in the same order.
 * If one day the order needs to be different for each Quick Search setting, we may need
 * a more complex data structure.
 */
data class QuickSearch(
    var name: String,
    var icon: String,
    var searchUrlPattern: String,
    var homeUrl: String,
    var urlPrefix: String = "",
    var urlSuffix: String = "",
    var patternEncode: Boolean = false,
    var permitSpace: Boolean = true
) {
    fun generateLink(keyword: String): String {
        var newKeyword = keyword

        // Remove all spaces in keyword
        if (!permitSpace) {
            newKeyword = keyword.replace("\\s".toRegex(), "")
        }

        val url = if (patternEncode) {
            Uri.encode(String.format(searchUrlPattern, newKeyword))
        } else {
            String.format(searchUrlPattern, Uri.encode(newKeyword))
        }
        return urlPrefix + url + urlSuffix
    }
}
