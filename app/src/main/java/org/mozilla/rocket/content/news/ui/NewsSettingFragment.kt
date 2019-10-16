package org.mozilla.rocket.content.news.ui

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dagger.Lazy
import org.mozilla.focus.R
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.getActivityViewModel
import org.mozilla.rocket.content.news.data.NewsLanguage
import javax.inject.Inject

class NewsSettingFragment : PreferenceFragmentCompat() {

    @Inject
    lateinit var applicationContext: Context

    @Inject
    lateinit var newsSettingsViewModelCreator: Lazy<NewsSettingsViewModel>

    private lateinit var newsSettingsViewModel: NewsSettingsViewModel

    private var languagePreference: NewsLanguagePreference? = null
    private var categoryPreference: NewsCategoryPreference? = null

    private var dialogHelper = LanguageListDialogHelper()

    private var langKey: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        newsSettingsViewModel = getActivityViewModel(newsSettingsViewModelCreator)
        newsSettingsViewModel.uiModel.observe(this, Observer { newsSettingsUiModel ->
            newsSettingsUiModel.preferenceLanguage.let { langChanged ->
                languagePreference?.summary = langChanged.name
                langKey = langChanged.apiId
            }
            newsSettingsUiModel.categories.let { categories ->
                categoryPreference?.updateCatList(categories)
            }

            dialogHelper.updateLangList(newsSettingsUiModel.allLanguages)
        })
    }

    override fun onPause() {
        super.onPause()
        categoryPreference?.NewsCatSettingCatAdapter()
    }

    override fun onCreatePreferences(p0: Bundle?, p1: String?) {
        addPreferencesFromResource(R.xml.settings_news)

        languagePreference = findPreference(PREF_NEWS_LANG) as? NewsLanguagePreference
        categoryPreference = findPreference(PREF_NEWS_CAT) as? NewsCategoryPreference
        categoryPreference?.onCategoryClick = {
            langKey?.let { key ->
                newsSettingsViewModel.updateUserPreferenceCategories(key, it)
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
        newsSettingsViewModel.updateUserPreferenceLanguage(language)
    }

    companion object {
        private const val PREF_NEWS_LANG = "pref_dummy_s_news_lang"
        private const val PREF_NEWS_CAT = "pref_dummy_s_news_Cat"

        fun newInstance(): NewsSettingFragment {
            return NewsSettingFragment()
        }
    }
}
