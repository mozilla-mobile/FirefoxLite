/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.content.news.data

import android.content.Context
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceViewHolder
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.ToggleButton
import org.mozilla.focus.R

class NewsCategoryPreference @JvmOverloads constructor(context: Context, attributes: AttributeSet? = null) :
    Preference(context, attributes) {

    init {
        layoutResource = R.layout.content_tab_new_setting
    }

    private var recyclerView: RecyclerView? = null
    private var progress: ProgressBar? = null
    private var categoryList: List<NewsCategory> = listOf()
    var onCategoryClick: (categories: List<NewsCategory>) -> Unit = {}

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        recyclerView = holder.findViewById(R.id.news_setting_cat_list) as RecyclerView
        progress = holder.findViewById(R.id.news_setting_cat_progress) as ProgressBar

        recyclerView?.adapter = NewsCatSettingCatAdapter()
        recyclerView?.layoutManager = GridLayoutManager(context, 2)

        if (categoryList.isNotEmpty()) {
            hideProgressBar()
        }
    }

    fun updateCatList(newList: List<NewsCategory>?) {
        if (newList == null || newList.isEmpty()) {
            return
        }
        categoryList = newList
        recyclerView?.adapter?.notifyDataSetChanged()

        hideProgressBar()
    }

    private fun hideProgressBar() {
        recyclerView?.visibility = View.VISIBLE
        progress?.visibility = View.GONE
    }

    inner class NewsCatSettingCatAdapter : RecyclerView.Adapter<CategorySettingItemViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, p1: Int): CategorySettingItemViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.content_tab_new_setting_item, parent, false)
            return CategorySettingItemViewHolder(v)
        }

        override fun onBindViewHolder(vh: CategorySettingItemViewHolder, pos: Int) {

            val displayString = vh.button.context.getString(categoryList[pos].stringResourceId)
            // the first item is always selected and not changeable
            if (pos == 0) {
                vh.button.isChecked = true
                vh.button.isEnabled = false
            } else {
                vh.button.isChecked = categoryList[pos].isSelected
                vh.button.isEnabled = true
            }
            vh.button.textOff = displayString
            vh.button.textOn = displayString
            vh.button.text = displayString

            vh.button.setOnClickListener {
                categoryList[pos].isSelected = !categoryList[pos].isSelected
                onCategoryClick(categoryList)
            }
        }

        override fun getItemCount(): Int {
            return categoryList.size
        }
    }
}

class CategorySettingItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    var button: ToggleButton = view.findViewById(R.id.news_setting_cat_item)
}
