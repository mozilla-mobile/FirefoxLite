/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_first_run.description
import kotlinx.android.synthetic.main.fragment_first_run.item_browsing
import kotlinx.android.synthetic.main.fragment_first_run.item_games
import kotlinx.android.synthetic.main.fragment_first_run.item_news
import kotlinx.android.synthetic.main.fragment_first_run.item_shopping
import org.mozilla.focus.R
import org.mozilla.focus.fragment.FirstrunFragment.ContentPrefItem.Browsing
import org.mozilla.focus.fragment.FirstrunFragment.ContentPrefItem.Games
import org.mozilla.focus.fragment.FirstrunFragment.ContentPrefItem.News
import org.mozilla.focus.fragment.FirstrunFragment.ContentPrefItem.Shopping
import org.mozilla.focus.navigation.ScreenNavigator.Screen

class FirstrunFragment : Fragment(), Screen {

    private var currentSelectedItem: ContentPrefItem? = null

    override fun getFragment(): Fragment {
        return this
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_first_run, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        description.text = getString(R.string.firstrun_fxlite_2_5_title_B, getString(R.string.app_name))
        initContentPrefItems()
    }

    private fun initContentPrefItems() {
        setContentPrefSelected(Browsing)

        item_browsing.setOnClickListener { setContentPrefSelected(Browsing) }
        item_shopping.setOnClickListener { setContentPrefSelected(Shopping) }
        item_games.setOnClickListener { setContentPrefSelected(Games) }
        item_news.setOnClickListener { setContentPrefSelected(News) }
    }

    private fun setContentPrefSelected(item: ContentPrefItem) {
        if (currentSelectedItem == item) return

        currentSelectedItem = item
        listOf(Browsing, Shopping, Games, News).groupBy { it == item }.run {
            get(true)?.forEach { view?.setContentPrefSelected(it, true) }
            get(false)?.forEach { view?.setContentPrefSelected(it, false) }
        }
    }

    private fun View.setContentPrefSelected(item: ContentPrefItem, selected: Boolean) {
        val view = this.findViewById<View>(item.viewId)
        val icon = view.findViewById<View>(item.iconId)
        val textView = view.findViewById<TextView>(item.textId)
        view.isSelected = selected
        icon.isVisible = selected
        val textColorId = if (selected) {
            R.color.paletteWhite100
        } else {
            R.color.paletteDarkGreyC100
        }
        textView.setTextColor(ContextCompat.getColor(context, textColorId))
    }

    private sealed class ContentPrefItem(val viewId: Int, val textId: Int, val iconId: Int) {
        object Browsing : ContentPrefItem(R.id.item_browsing, R.id.text_browsing, R.id.icon_browsing)
        object Shopping : ContentPrefItem(R.id.item_shopping, R.id.text_shopping, R.id.icon_shopping)
        object Games : ContentPrefItem(R.id.item_games, R.id.text_games, R.id.icon_games)
        object News : ContentPrefItem(R.id.item_news, R.id.text_news, R.id.icon_news)
    }

    // TODO: Evan, add back to version 2.5 onboarding
//    private fun wrapButtonClickListener(onClickListener: View.OnClickListener): View.OnClickListener {
//        return View.OnClickListener { view ->
//            if (view.id == R.id.finish) {
//                activity?.sendBroadcast(Intent(activity, PeriodicReceiver::class.java).apply {
//                    action = FirstLaunchWorker.ACTION
//                })
//            }
//            onClickListener.onClick(view)
//        }
//    }
//
//    private fun finishFirstrun() {
//        NewFeatureNotice.getInstance(context).setFirstRunDidShow()
//        NewFeatureNotice.getInstance(context).setLiteUpdateDidShow()
//        (activity as MainActivity).firstrunFinished()
//    }

    companion object {
        @JvmStatic
        fun create(): FirstrunFragment {
            return FirstrunFragment()
        }
    }
}
