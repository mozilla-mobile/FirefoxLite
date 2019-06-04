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

    private var catList: RecyclerView? = null
    private var progress: ProgressBar? = null

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        catList = holder.findViewById(R.id.news_setting_cat_list) as RecyclerView
        progress = holder.findViewById(R.id.news_setting_cat_progress) as ProgressBar

        val cats = listOf<NewsCategory>()
        catList?.adapter = NewsCatSettingCatAdapter(cats)
        catList?.layoutManager = GridLayoutManager(context, 2)
    }

    fun updateCatList(newList: List<NewsCategory>?) {
        if (newList == null) {
            return
        }

        catList?.visibility = View.GONE
        progress?.visibility = View.VISIBLE

        (catList?.adapter as? NewsCatSettingCatAdapter)?.apply {

            this.cats = newList
            this.notifyDataSetChanged()
            catList?.visibility = View.VISIBLE
            progress?.visibility = View.GONE
        }
    }

    fun getCategoryList() = (catList?.adapter as? NewsCatSettingCatAdapter)?.cats
}

class NewsCatSettingCatAdapter(var cats: List<NewsCategory>) :
    RecyclerView.Adapter<CategorySettingItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, p1: Int): CategorySettingItemViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.content_tab_new_setting_item, parent, false)
        return CategorySettingItemViewHolder(v)
    }

    override fun onBindViewHolder(vh: CategorySettingItemViewHolder, pos: Int) {
        vh.button.textOff = cats[pos].categoryId
        vh.button.textOn = cats[pos].categoryId
        vh.button.text = cats[pos].categoryId
        vh.button.isChecked = cats[pos].isSelected
        vh.button.setOnCheckedChangeListener { _, checked ->
            cats[pos].isSelected = checked
        }
    }

    override fun getItemCount(): Int {
        return cats.size
    }
}

class CategorySettingItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    var button: ToggleButton = view.findViewById(R.id.news_setting_cat_item)
}
