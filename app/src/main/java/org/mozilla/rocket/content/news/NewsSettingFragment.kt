package org.mozilla.rocket.content.news

import android.os.Bundle
import android.support.v7.preference.PreferenceFragmentCompat
import org.mozilla.focus.R

class NewsSettingFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(p0: Bundle?, p1: String?) {
        addPreferencesFromResource(R.xml.settings_news)
    }

    companion object {
        fun newInstance(): NewsSettingFragment {
            return NewsSettingFragment()
        }
    }
}