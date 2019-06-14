package org.mozilla.rocket.content.news

import androidx.lifecycle.Observer
import android.content.Context
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import android.util.Log
import android.view.View
import dagger.android.support.AndroidSupportInjection
import org.mozilla.focus.R
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.content.news.data.NewsCategory
import org.mozilla.rocket.content.news.data.NewsCategoryPreference
import org.mozilla.rocket.content.news.data.NewsLanguage
import org.mozilla.rocket.content.news.data.NewsLanguagePreference
import org.mozilla.rocket.content.news.data.NewsSettingsRepository

class NewsSettingFragment : PreferenceFragmentCompat() {

    @javax.inject.Inject
    lateinit var applicationContext: Context

    @javax.inject.Inject
    lateinit var repository: NewsSettingsRepository

    private var languagePreference: NewsLanguagePreference? = null
    private var categoryPreference: NewsCategoryPreference? = null

    private var dialogHelper = LanguageListDialogHelper()

    private var langKey: String? = null

    companion object {
        private const val TAG = "NewsSettingFragment"
        private const val PREF_NEWS_LANG = "pref_dummy_s_news_lang"
        private const val PREF_NEWS_CAT = "pref_dummy_s_news_Cat"

        fun newInstance(): NewsSettingFragment {
            return NewsSettingFragment()
        }
    }

    override fun onPause() {
        super.onPause()
        categoryPreference?.NewsCatSettingCatAdapter()
        val catList = categoryPreference?.getCatList()
        TelemetryWrapper.changeNewsSetting(categories = catList?.filter { item -> item.isSelected }?.map { lang -> lang.order.toString() })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AndroidSupportInjection.inject(this)

        val allLangsObserver = Observer<List<NewsLanguage>> {
            Log.d(TAG, "language list has changed")
            dialogHelper.updateLangList(it)
        }
        repository.getLanguages().observe(viewLifecycleOwner, allLangsObserver)

        val settingObserver = Observer<Pair<NewsLanguage, List<NewsCategory>>> {
            Log.d(
                TAG,
                "news locale/cats setting has changed, hence the changes to the cat list is overridden again and again "
            )
            it?.first?.let { langChanged ->
                languagePreference?.summary = langChanged.name
                langKey = langChanged.getApiId()
            }
            it?.second?.let { categories ->
                categoryPreference?.updateCatList(categories)
            }
        }
        repository.getNewsSettings().observe(viewLifecycleOwner, settingObserver)
    }

    override fun onCreatePreferences(p0: Bundle?, p1: String?) {
        addPreferencesFromResource(R.xml.settings_news)

        languagePreference = findPreference(PREF_NEWS_LANG) as? NewsLanguagePreference
        categoryPreference = findPreference(PREF_NEWS_CAT) as? NewsCategoryPreference
        categoryPreference?.onCategoryClick = {
            langKey?.let { key ->
                repository.setUserPreferenceCategories(key, it)
            }
        }
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        if (preference?.key == PREF_NEWS_LANG) {
            onLanguagePrefClick()
        }
        return super.onPreferenceTreeClick(preference)
    }

    private fun onLanguagePrefClick() {
        dialogHelper.build(context!!, ::setUserPreferLanguage)
    }

    private fun setUserPreferLanguage(language: NewsLanguage) {
        TelemetryWrapper.changeNewsSetting(language = language.key)
        repository.setUserPreferenceLanguage(language)
    }
}