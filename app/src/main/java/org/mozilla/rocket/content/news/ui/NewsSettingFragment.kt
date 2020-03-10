package org.mozilla.rocket.content.news.ui

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dagger.Lazy
import org.mozilla.focus.R
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.getActivityViewModel
import org.mozilla.rocket.content.news.data.NewsCategory
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
    private var personalizedNewsPreference: PersonalizedNewsPreference? = null

    private var dialogHelper = LanguageListDialogHelper()

    private var langKey: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        newsSettingsViewModel = getActivityViewModel(newsSettingsViewModelCreator)
        newsSettingsViewModel.uiModel.observe(viewLifecycleOwner, Observer { newsSettingsUiModel ->
            newsSettingsUiModel.preferenceLanguage.let { langChanged ->
                languagePreference?.summary = langChanged.name
                langKey = langChanged.apiId
            }
            newsSettingsUiModel.categories.let { categories ->
                categoryPreference?.updateCatList(categories)
            }

            dialogHelper.updateLangList(newsSettingsUiModel.allLanguages)
        })
        newsSettingsViewModel.showPersonalizedNewsSetting.observe(viewLifecycleOwner, Observer {
            personalizedNewsPreference?.isVisible = true
            personalizedNewsPreference?.isChecked = it
        })
        newsSettingsViewModel.personalizedNewsSettingChanged.observe(viewLifecycleOwner, Observer {
            context?.let {
                startActivity(NewsActivity.getStartIntent(it).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                })
            }
        })
    }

    override fun onPause() {
        super.onPause()
        categoryPreference?.NewsCatSettingCatAdapter()
    }

    override fun onStop() {
        super.onStop()
        newsSettingsViewModel.onLeaveSettingsPage()
    }

    override fun onCreatePreferences(p0: Bundle?, p1: String?) {
        addPreferencesFromResource(R.xml.settings_news)

        languagePreference = findPreference(PREF_NEWS_LANG) as? NewsLanguagePreference
        categoryPreference = findPreference(PREF_NEWS_CAT) as? NewsCategoryPreference
        categoryPreference?.onCategoryClick = { effectCategory: NewsCategory, allCategories: List<NewsCategory> ->
            langKey?.let { key ->
                newsSettingsViewModel.updateUserPreferenceCategories(key, effectCategory, allCategories)
            }
        }

        personalizedNewsPreference = findPreference(PREF_PERSONALIZED_NEWS) as? PersonalizedNewsPreference
        personalizedNewsPreference?.isVisible = false
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        if (preference?.key == PREF_NEWS_LANG) {
            onLanguagePrefClick()
        } else if (preference?.key == PREF_PERSONALIZED_NEWS) {
            onPersonalizedNewsClick()
        }
        return super.onPreferenceTreeClick(preference)
    }

    private fun onLanguagePrefClick() {
        dialogHelper.build(context!!, ::setUserPreferLanguage)
    }

    private fun onPersonalizedNewsClick() {
        personalizedNewsPreference?.let {
            if (it.isChecked) {
                showEnablePersonalizedNewsDialog()
            } else {
                showDisablePersonalizedNewsDialog()
            }
        }
    }

    private fun setUserPreferLanguage(language: NewsLanguage) {
        newsSettingsViewModel.updateUserPreferenceLanguage(language)
    }

    private fun showEnablePersonalizedNewsDialog() {
        AlertDialog.Builder(context)
            .setMessage(R.string.recommended_news_preference_enable_dialog)
            .setPositiveButton(android.R.string.yes) { _, _ ->
                newsSettingsViewModel.togglePersonalizedNewsSwitch(true)
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                personalizedNewsPreference?.isChecked = false
            }
            .setOnCancelListener {
                personalizedNewsPreference?.isChecked = false
            }
            .show()
    }

    private fun showDisablePersonalizedNewsDialog() {
        AlertDialog.Builder(context)
            .setMessage(R.string.recommended_news_preference_disable_dialog)
            .setPositiveButton(android.R.string.yes) { _, _ ->
                newsSettingsViewModel.togglePersonalizedNewsSwitch(false)
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                personalizedNewsPreference?.isChecked = true
            }
            .setOnCancelListener {
                personalizedNewsPreference?.isChecked = true
            }
            .show()
    }

    companion object {
        private const val PREF_NEWS_LANG = "pref_dummy_s_news_lang"
        private const val PREF_NEWS_CAT = "pref_dummy_s_news_Cat"
        private const val PREF_PERSONALIZED_NEWS = "pref_dummy_s_personalized_news"

        fun newInstance(): NewsSettingFragment {
            return NewsSettingFragment()
        }
    }
}
