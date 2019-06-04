package org.mozilla.rocket.content.news

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.content.Context
import android.os.Bundle
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import android.view.View
import dagger.android.support.AndroidSupportInjection
import org.mozilla.focus.R
import org.mozilla.rocket.content.news.data.NewsCategory
import org.mozilla.rocket.content.news.data.NewsCategoryPreference
import org.mozilla.rocket.content.news.data.NewsLanguagePreference
import org.mozilla.rocket.content.news.data.NewsSettingsRepository
import org.mozilla.rocket.extension.switchMap

class NewsSettingFragment : PreferenceFragmentCompat() {

    @javax.inject.Inject
    lateinit var applicationContext: Context

    @javax.inject.Inject
    lateinit var repository: NewsSettingsRepository

    var queryCatsByLang = MutableLiveData<String>()
    var categories: LiveData<List<NewsCategory>> = queryCatsByLang.switchMap {
        repository.getCategoriesByLanguage(it)
    }

    private var languagePreference: NewsLanguagePreference? = null
    private var categoryPreference: NewsCategoryPreference? = null

    private var dialogHelper = LanguageListDialogHelper()

    companion object {
        private const val PREF_NEWS_LANG = "pref_dummy_s_news_lang"
        private const val PREF_NEWS_CAT = "pref_dummy_s_news_Cat"

        fun newInstance(): NewsSettingFragment {
            return NewsSettingFragment()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AndroidSupportInjection.inject(this)
        repository.getUserPreferenceLanguage().observe(viewLifecycleOwner, Observer {
            if (it != null) {
                queryCatsByLang.value = it.key.toLowerCase()
                languagePreference?.summary = it.name
            }
        })

        repository.getLanguages().observe(viewLifecycleOwner, Observer {
            dialogHelper.updateLangList(it)
        })
        categories.observe(viewLifecycleOwner, Observer {
            categoryPreference?.updateCatList(it)
        })
    }

    override fun onCreatePreferences(p0: Bundle?, p1: String?) {
        addPreferencesFromResource(R.xml.settings_news)

        languagePreference = findPreference(PREF_NEWS_LANG) as? NewsLanguagePreference
        categoryPreference = findPreference(PREF_NEWS_CAT) as? NewsCategoryPreference
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        if (preference?.key == PREF_NEWS_LANG) {
            onLanguagePrefClick()
        }
        return super.onPreferenceTreeClick(preference)
    }

    // FIXME: data in view, bad

    private fun onLanguagePrefClick() {
        dialogHelper.build(context!!)
    }
}