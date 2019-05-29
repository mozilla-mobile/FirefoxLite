/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.content.news.data

import android.content.Context
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
import android.widget.TextView
import org.mozilla.focus.R
import org.mozilla.threadutils.ThreadUtils

class NewsLanguagePreference @JvmOverloads constructor(context: Context, attributes: AttributeSet? = null) :
    Preference(context, attributes) {

    override fun getSummary(): CharSequence? {

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        if (sharedPreferences.getString("lang", null) == null) {
            return "default lang"
        }
        return "english"
    }

    override fun onClick() {
        super.onClick()
        val layoutInflater = LayoutInflater.from(context)
        val view = layoutInflater.inflate(R.layout.simple_list, null)
        val langs = listOf(
            NewsLanguage("key", "code", "name", true),
            NewsLanguage("key2", "code2", "name2", false)
        )
        val adapter = object : BaseAdapter() {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val item = layoutInflater.inflate(android.R.layout.simple_list_item_1, null)
                item.findViewById<TextView>(android.R.id.text1).text = langs[position].name
                return item
            }

            override fun getItem(position: Int): Any {
                return langs[position]
            }

            override fun getItemId(position: Int): Long {
                return position.toLong()
            }

            override fun getCount(): Int {
                return langs.size
            }
        }
        val list = view.findViewById<ListView>(R.id.simple_list)
        val progress = view.findViewById<ProgressBar>(R.id.simple_progress)

        val dialog = AlertDialog.Builder(context)
            .setNegativeButton(
                R.string.setting_dialog_cancel
            ) { dialog, _ -> dialog.dismiss() }
            .setView(view).create()
        ThreadUtils.postToBackgroundThread {
            Thread.sleep(3000)
            ThreadUtils.postToMainThread {
                list.adapter = adapter
                adapter.notifyDataSetChanged()
                progress.visibility = View.GONE
                list.visibility = View.VISIBLE
            }
        }
        dialog.show()
    }
}
