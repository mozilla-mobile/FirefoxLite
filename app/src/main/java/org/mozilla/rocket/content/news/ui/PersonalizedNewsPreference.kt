/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.rocket.content.news.ui

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.TextView
import androidx.preference.PreferenceViewHolder
import org.mozilla.focus.activity.InfoActivity
import org.mozilla.focus.utils.SupportUtils

class PersonalizedNewsPreference @JvmOverloads constructor(context: Context, attributes: AttributeSet? = null) :
    androidx.preference.SwitchPreference(context, attributes) {

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val summary = holder.findViewById(android.R.id.summary) as TextView
        val typedValue = TypedValue()
        val ta = context.obtainStyledAttributes(typedValue.data, intArrayOf(android.R.attr.textColorLink))
        val color = ta.getColor(0, 0)
        ta.recycle()
        summary.setTextColor(color)
        summary.setOnClickListener {
            // This is a hardcoded link: if we ever end up needing more of these links, we should
            // move the link into an xml parameter, but there's no advantage to making it configurable now.
            val url = SupportUtils.getSumoURLForTopic(context, "enable-news-lite")
            val title = title.toString()
            val intent = InfoActivity.getIntentFor(context, url, title)
            context.startActivity(intent)
        }
    }
}