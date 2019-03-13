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

    var loadMoreListener: LoadMoreListener? = null

    private var recyclerView: RecyclerView? = null
    private var emptyView: View? = null
    private var progressCenter: ProgressBar? = null
    private var adapter: ContentAdapter<NewsItem>? = null
    private var bottomSheet: View? = null
    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null

    interface LoadMoreListener {
        fun loadMore()
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

        if (HomeFragmentViewState.TYPE_NEWS == HomeFragmentViewState.state) {
            show(false)
            HomeFragmentViewState.state = HomeFragmentViewState.TYPE_DEFAULT
        }
    }

    private var linearLayoutManager: LinearLayoutManager? = null

    private fun setupView() {
        this.setOnClickListener { hide() }
        findViewById<Button>(R.id.news_try_again)?.setOnClickListener {
            loadMoreListener?.loadMore()
        }
        recyclerView = findViewById(R.id.recyclerview)
        emptyView = findViewById(R.id.empty_view_container)
        progressCenter = findViewById(R.id.news_progress_center)
        adapter = ContentAdapter(this)
        adapter?.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                if (itemCount == 0) {
                    onStatus(VIEW_TYPE_EMPTY)
                } else {
                    onStatus(VIEW_TYPE_NON_EMPTY)
                }
            }
        })
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
                            loadMoreListener?.loadMore()
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
        val anim = AnimationUtils.loadAnimation(context, R.anim.tab_transition_fade_out)
        anim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
            }

            override fun onAnimationRepeat(animation: Animation?) {
            }

            override fun onAnimationEnd(animation: Animation?) {
                visibility = GONE
            }
        })

        this.startAnimation(anim)
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

    override fun onStatus(status: Int) {
        when (status) {
            VIEW_TYPE_EMPTY -> {
                recyclerView?.visibility = View.GONE
                emptyView?.visibility = View.VISIBLE
                progressCenter?.visibility = View.GONE
            }
            VIEW_TYPE_NON_EMPTY -> {
                recyclerView?.visibility = View.VISIBLE
                emptyView?.visibility = View.GONE
                progressCenter?.visibility = View.GONE
            }
            else -> {
                recyclerView?.visibility = View.GONE
                emptyView?.visibility = View.GONE
            }
        }
    }

    override fun onItemClicked(url: String) {
        ScreenNavigator.get(context).showBrowserScreen(url, true, false)

        // use findFirstVisibleItemPosition so we don't need to remember offset
        linearLayoutManager?.findFirstVisibleItemPosition()?.let {
            HomeFragmentViewState.lastScrollPos = it
            // remember the state, so the next time will open news portal
            HomeFragmentViewState.state = HomeFragmentViewState.TYPE_NEWS
        }
    }

    fun setData(items: MutableList<NewsItem>?) {
        bottomSheetBehavior?.skipCollapsed = items == null || items.size == 0
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

    companion object {
        private const val NEWS_THRESHOLD = 10
        const val VIEW_TYPE_EMPTY = 0
        const val VIEW_TYPE_NON_EMPTY = 1
    }
}