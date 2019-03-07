/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import kotlinx.android.synthetic.main.fragment_news.*
import org.mozilla.focus.R
import org.mozilla.focus.navigation.ScreenNavigator
import org.mozilla.rocket.content.ContentAdapter
import org.mozilla.rocket.content.ContentRepository
import org.mozilla.rocket.content.ContentViewModel
import org.mozilla.rocket.content.HomeFragmentViewState
import org.mozilla.threadutils.ThreadUtils

class NewsFragment : PanelFragment(), ContentAdapter.ContentPanelListener {

    private var recyclerView: RecyclerView? = null
    private var emptyView: View? = null
    private var progressView: View? = null
    private var adapter: ContentAdapter? = null
    private var viewModel: ContentViewModel? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_news, container, false)
        recyclerView = v.findViewById(R.id.recyclerview)
        emptyView = v.findViewById(R.id.empty_view_container)
        recyclerView?.clipToPadding = false
        progressView = v.findViewById(R.id.news_progress)
        v.findViewById<Button>(R.id.news_try_again).setOnClickListener {
            viewModel?.loadMore()
        }
        return v
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val layoutManager = LinearLayoutManager(activity)
        adapter = ContentAdapter(this)
        recyclerView?.adapter = adapter
        recyclerView?.layoutManager = layoutManager

        setupContentViewModel()

        // let news_init_loading display a bit till fetch completes
    }

    override fun tryLoadMore() {
        ThreadUtils.postToMainThread {
            if (progressView?.visibility != View.VISIBLE) {
                progressView?.visibility = View.VISIBLE
                viewModel?.loadMore()
            }
        }
    }

    override fun onStatus(status: Int) {
        if (PanelFragment.VIEW_TYPE_EMPTY == status) {
            news_init_loading?.visibility = View.GONE
            recyclerView?.visibility = View.GONE
            emptyView?.visibility = View.VISIBLE
        } else if (PanelFragment.VIEW_TYPE_NON_EMPTY == status) {
            news_init_loading?.visibility = View.GONE
            recyclerView?.visibility = View.VISIBLE
            emptyView?.visibility = View.GONE
        } else {
            recyclerView?.visibility = View.GONE
            emptyView?.visibility = View.GONE
        }
    }

    override fun onItemClicked(url: String) {
        ScreenNavigator.get(context).showBrowserScreen(url, true, false)
        HomeFragmentViewState.set(ListPanelDialog.TYPE_NEWS)
        closePanel()
    }

    private fun setupContentViewModel() {
        activity?.also {
            viewModel = ViewModelProviders.of(it).get(ContentViewModel::class.java)
            val repository = ContentRepository.getInstance(context)
            repository.setOnDataChangedListener(viewModel)
            viewModel?.repository = repository
            viewModel?.items?.observe(
                    viewLifecycleOwner,
                    Observer { items ->
                    if (items == null) {
                        // load the items on first subscribe
                        viewModel?.loadMore()
                    } else {
                        progressView?.visibility = View.GONE
                        adapter?.setData(items)
                    }
                })
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(): NewsFragment {
            return NewsFragment()
        }

        private const val PREF_KEY_BOOL_NEWS = "pref_key_bool_news"

        @JvmStatic
        fun isEnable(context: Context?): Boolean {
            return context?.let {
                PreferenceManager.getDefaultSharedPreferences(context)
                        .getBoolean(PREF_KEY_BOOL_NEWS, false)
            } ?: false
        }
    }
}
