package org.mozilla.rocket.content.news

import android.content.SharedPreferences
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v7.preference.PreferenceFragmentCompat
import android.support.v7.widget.RecyclerView
import org.mozilla.focus.R
import org.mozilla.rocket.content.news.data.CategorySetting
import org.mozilla.rocket.content.news.data.NewsCatSettingCatAdapter

class NewsSettingFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        fun newInstance(): NewsSettingFragment {
            return NewsSettingFragment()
        }
    }

    override fun onCreatePreferences(p0: Bundle?, p1: String?) {
        addPreferencesFromResource(R.xml.settings_news)
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()

        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == "test2") {
            val list = (context as? FragmentActivity)?.findViewById<RecyclerView>(R.id.news_setting_cat_list)
            (list?.adapter as? NewsCatSettingCatAdapter)?.apply {
                this.cats = listOf(CategorySetting("D", "DD"), CategorySetting("E", "EE"), CategorySetting("F", "FF"))
                this.notifyDataSetChanged()
            }
        }
    }
}