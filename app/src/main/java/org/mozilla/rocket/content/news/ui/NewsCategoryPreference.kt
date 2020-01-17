/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.content.news.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.ToggleButton
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.focus.R
import org.mozilla.rocket.content.news.data.NewsCategory

class NewsCategoryPreference @JvmOverloads constructor(context: Context, attributes: AttributeSet? = null) :
    Preference(context, attributes) {

    init {
        layoutResource = R.layout.preference_news_category
    }

    private var recyclerView: RecyclerView? = null
    private var progress: ProgressBar? = null
    private var categoryList: List<NewsCategory> = listOf()
    var onCategoryClick: (effectCategory: NewsCategory, allCategories: List<NewsCategory>) -> Unit = { _: NewsCategory, _: List<NewsCategory> -> }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        recyclerView = holder.findViewById(R.id.news_setting_cat_list) as RecyclerView
        progress = holder.findViewById(R.id.news_setting_cat_progress) as ProgressBar

        recyclerView?.adapter = NewsCatSettingCatAdapter()
        val spanCount = context.resources.getInteger(R.integer.news_setting_category_column)
        recyclerView?.layoutManager = GridLayoutManager(context, spanCount)

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
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_news_category_preference, parent, false)
            return CategorySettingItemViewHolder(v)
        }

        override fun onBindViewHolder(vh: CategorySettingItemViewHolder, pos: Int) {

            val displayString = if (categoryList[pos].stringResourceId != 0) {
                vh.button.context.getString(categoryList[pos].stringResourceId)
            } else {
                categoryList[pos].name
            }
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
                onCategoryClick(categoryList[pos], categoryList)
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
