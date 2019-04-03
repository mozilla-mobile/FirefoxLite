/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.content

import android.content.Context
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.CoordinatorLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ProgressBar
import org.mozilla.focus.R
import org.mozilla.focus.navigation.ScreenNavigator
import org.mozilla.lite.partner.NewsItem

class ContentPortalView : CoordinatorLayout, ContentAdapter.ContentPanelListener {

    var newsListListener: NewsListListener? = null

    private var recyclerView: RecyclerView? = null
    private var emptyView: View? = null
    private var progressCenter: ProgressBar? = null
    private var adapter: ContentAdapter<NewsItem>? = null
    private var bottomSheet: View? = null
    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null

    interface NewsListListener {
        fun loadMore()
        fun onShow(context: Context)
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
            context, attrs, defStyleAttr
    )

    // this is called when HomeFragment is created. Since this view always attach to HomeFragment
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        init()
    }

    private fun init() {

        setupBottomSheet()

        setupView()
    }

    private var linearLayoutManager: LinearLayoutManager? = null

    private fun setupView() {
        this.setOnClickListener { hide() }
        findViewById<Button>(R.id.news_try_again)?.setOnClickListener {
            newsListListener?.loadMore()
        }
        recyclerView = findViewById(R.id.recyclerview)
        emptyView = findViewById(R.id.empty_view_container)
        progressCenter = findViewById(R.id.news_progress_center)
        adapter = ContentAdapter(this)

        recyclerView?.adapter = adapter
        linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        recyclerView?.layoutManager = linearLayoutManager
        linearLayoutManager?.let {
            recyclerView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val totalItemCount = it.itemCount
                    val visibleItemCount = it.childCount
                    val lastVisibleItem = it.findLastVisibleItemPosition()
                    if (visibleItemCount + lastVisibleItem + NEWS_THRESHOLD >= totalItemCount) {
                            newsListListener?.loadMore()
                    }
                }
            })
        }
    }

    fun showInternal() {
        visibility = VISIBLE
        // TODO: if previously is collapsed, collapse here.
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun show(animated: Boolean) {
        if (visibility == VISIBLE) {
            return
        }
        HomeFragmentViewState.lastOpenNews()
        newsListListener?.onShow(context)

        if (!animated) {
            showInternal()
            return
        }

        AnimationUtils.loadAnimation(context, R.anim.tab_transition_fade_in)?.also {
            it.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                }

                override fun onAnimationRepeat(animation: Animation?) {
                }

                override fun onAnimationEnd(animation: Animation?) {
                    showInternal()
                }
            })
            this.startAnimation(it)
        }
    }

    fun hide(): Boolean {
        if (visibility == GONE) {
            return false
        }

        HomeFragmentViewState.reset()
        ContentRepository.reset()
        AnimationUtils.loadAnimation(context, R.anim.tab_transition_fade_out)?.also {
            it.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                }

                override fun onAnimationRepeat(animation: Animation?) {
                }

                override fun onAnimationEnd(animation: Animation?) {
                    visibility = GONE
                }
            })
            this.startAnimation(it)
        }
        return true
    }

    private fun setupBottomSheet() {
        bottomSheet = findViewById(R.id.bottom_sheet)
        bottomSheetBehavior = BottomSheetBehavior.from<View>(bottomSheet)
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheetBehavior?.setBottomSheetCallback(object :
                BottomSheetBehavior.BottomSheetCallback() {

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    hide()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // we don't want to slide other stuff here
            }
        })
    }

    override fun onStatus(items: MutableList<out NewsItem>?) {
        when {
            items == null -> {
                recyclerView?.visibility = View.GONE
                emptyView?.visibility = View.GONE
                progressCenter?.visibility = View.VISIBLE
                bottomSheetBehavior?.skipCollapsed = true
            }
            items.size == 0 -> {
                recyclerView?.visibility = View.GONE
                emptyView?.visibility = View.VISIBLE
                progressCenter?.visibility = View.GONE
                bottomSheetBehavior?.skipCollapsed = true
            }
            else -> {
                recyclerView?.visibility = View.VISIBLE
                emptyView?.visibility = View.GONE
                progressCenter?.visibility = View.GONE
            }
        }
    }

    override fun onItemClicked(url: String) {
        ScreenNavigator.get(context).showBrowserScreen(url, true, false)

        // use findFirstVisibleItemPosition so we don't need to remember offset
        linearLayoutManager?.findFirstVisibleItemPosition()?.let {
            HomeFragmentViewState.lastScrollPos = it
        }
    }

    fun setData(items: MutableList<out NewsItem>?) {

        onStatus(items)

        adapter?.submitList(items)
        HomeFragmentViewState.lastScrollPos?.let {
            val size = items?.size
            if (size != null && size > it) {
                linearLayoutManager?.scrollToPosition(it)
                // forget about last scroll position
                HomeFragmentViewState.lastScrollPos = null
            }
        }
    }

    fun onResume() {
        if (HomeFragmentViewState.isLastOpenNews()) {
            show(false)
        } else {
            hide()
        }
    }

    companion object {
        private const val NEWS_THRESHOLD = 10
    }
}