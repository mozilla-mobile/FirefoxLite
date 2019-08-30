package org.mozilla.rocket.content.news.ui

import android.annotation.SuppressLint
import android.content.Context
import android.database.DataSetObserver
import androidx.appcompat.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.RadioButton
import org.mozilla.focus.R
import org.mozilla.rocket.content.news.data.NewsLanguage

class LanguageListDialogHelper {

    private var dialogAdapter: BaseAdapter? = null

    val languages = mutableListOf<NewsLanguage>() // empty list

    fun build(context: Context, onclick: (NewsLanguage) -> Unit) {

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
                    dialogAdapter?.notifyDataSetChanged()
                    // TODO: repository.setUserPreferenceLanguage(languages[position])
                    onclick(languages[position])
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