package org.mozilla.rocket.content.news

import android.annotation.SuppressLint
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.content.Context
import android.database.DataSetObserver
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.RadioButton
import dagger.android.support.AndroidSupportInjection
import org.mozilla.focus.R
import org.mozilla.rocket.content.news.data.NewsCategory
import org.mozilla.rocket.content.news.data.NewsCategoryPreference
import org.mozilla.rocket.content.news.data.NewsLanguage
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

    var languagePreference: NewsLanguagePreference? = null
    var categoryPreference: NewsCategoryPreference? = null

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
            updateLangList(it)
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
    val languages = mutableListOf<NewsLanguage>() // empty list

    private var dialogAdapter: BaseAdapter? = null
    fun onLanguagePrefClick() {
        val layoutInflater = LayoutInflater.from(context)
        val view = layoutInflater.inflate(R.layout.simple_list, null)
        // this is the time when the view is lay-out
        val dialogList = view?.findViewById(R.id.simple_list) as? ListView
        val dialogProgress = view?.findViewById(R.id.simple_progress) as? ProgressBar
        val dialog1 = AlertDialog.Builder(context!!)
            .setTitle(R.string.news_language_dialog_title)
            .setNegativeButton(
                R.string.setting_dialog_cancel
            ) { dialog, _ -> dialog.dismiss() }
            .setView(view).create()
        dialogAdapter = object : BaseAdapter() {
            @SuppressLint("ViewHolder") // be easy here.
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val item = layoutInflater.inflate(R.layout.simple_list_check_item, null)
                val tv = item.findViewById<RadioButton>(R.id.simple_list_check_text)
                tv.text = languages[position].name
                tv.isChecked = languages[position].isSelected
                item.setOnClickListener {
                    languages.forEach { it.isSelected = false }
                    languages[position].isSelected = true
                    dialogAdapter?.notifyDataSetChanged()
                    repository.setUserPreferenceLanguage(languages[position])

                    dialog1.dismiss()
                    // save db value, update pref, relo
                }
                return item
            }

            override fun getItem(position: Int): Any {
                return languages[position]
            }

            override fun getItemId(position: Int): Long {
                return position.toLong()
            }

            override fun getCount(): Int {
                return languages.size
            }
        }
        dialogAdapter?.registerDataSetObserver(object : DataSetObserver() {
            override fun onChanged() {
                super.onChanged()
                // FIXME: extract this
                if (languages.size > 0) {
                    dialogProgress?.visibility = View.GONE
                    dialogList?.visibility = View.VISIBLE
                }
            }
        })
        dialogList?.adapter = dialogAdapter

        dialog1.show()
        // FIXME: extract this
        if (languages.size > 0) {
            dialogProgress?.visibility = View.GONE
            dialogList?.visibility = View.VISIBLE
        }
    }

    fun updateLangList(list: List<NewsLanguage>?) {
        list?.let {
            if (list == languages) {
                return
            }
            languages.clear()
            languages.addAll(list)
            dialogAdapter?.notifyDataSetChanged()
        }
    }
}