/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.content.news.data

import android.content.Context
import android.support.v7.preference.Preference
import android.util.AttributeSet

// dummy preference for clicking
class NewsLanguagePreference @JvmOverloads constructor(context: Context, attributes: AttributeSet? = null) :
    Preference(context, attributes)
