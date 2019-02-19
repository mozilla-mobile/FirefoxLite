/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.rocket.urlinput

import android.net.Uri

data class QuickSearch(
    var name: String,
    var icon: String,
    var searchUrlPattern: String,
    var homeUrl: String,
    var urlPrefix: String = "",
    var urlSuffix: String = "",
    var patternEncode: Boolean = false
) {
    fun generateLink(keyword: String): String {
        var url = if (patternEncode) {
            Uri.encode(String.format(searchUrlPattern, keyword))
        } else {
            String.format(searchUrlPattern, Uri.encode(keyword))
        }
        return urlPrefix + url + urlSuffix
    }
}
