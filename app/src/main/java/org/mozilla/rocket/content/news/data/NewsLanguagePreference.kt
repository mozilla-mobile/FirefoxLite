/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.content.news.data

import android.annotation.SuppressLint
import android.content.Context
import android.database.DataSetObserver
import android.support.v7.app.AlertDialog
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.RadioButton
import org.mozilla.focus.R

class NewsLanguagePreference @JvmOverloads constructor(context: Context, attributes: AttributeSet? = null) :
    Preference(context, attributes) {

    // FIXME: data in view, bad
    val languages = mutableListOf<NewsLanguage>() // empty list

    override fun getSummary(): CharSequence? {

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        return sharedPreferences.getString("user_pref_lang", null) ?: return "default lang"
    }

    private var dialogAdapter: BaseAdapter? = null

    @SuppressLint("InflateParams")
    override fun onClick() {
        super.onClick()
        val layoutInflater = LayoutInflater.from(context)
        val view = layoutInflater.inflate(R.layout.simple_list, null)
        // this is the time when the view is lay-out
        val dialogList = view?.findViewById(R.id.simple_list) as? ListView
        val dialogProgress = view?.findViewById(R.id.simple_progress) as? ProgressBar
        val dialog1 = AlertDialog.Builder(context)
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
                    notifyDataSetChanged()
                    PreferenceManager.getDefaultSharedPreferences(context).edit()
                        .putString("user_pref_lang", languages[position].key).apply()
                    summary = languages[position].name
                    notifyChanged()
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
