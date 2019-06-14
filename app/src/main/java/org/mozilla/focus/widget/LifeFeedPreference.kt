package org.mozilla.focus.widget

import android.content.Context
import android.preference.ListPreference
import android.util.AttributeSet

import org.mozilla.focus.R

class LifeFeedPreference : ListPreference {
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onAttachedToActivity() {
        super.onAttachedToActivity()
        summary = summary
    }

    override fun getSummary(): CharSequence {
        var selectionIndex = 0
        if (value != null) {
            try {
                selectionIndex = Integer.parseInt(value)
            } catch (e: NumberFormatException) {
                e.printStackTrace()
            }
        }

        val settings = context.resources.getStringArray(R.array.life_feed_settings_entries)
        if (selectionIndex >= settings.size) {
            selectionIndex = 0
        }

        return settings[selectionIndex]
    }
}
