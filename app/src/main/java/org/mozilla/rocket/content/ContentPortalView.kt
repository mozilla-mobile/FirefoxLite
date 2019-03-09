/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.content

import android.content.Context
import android.preference.PreferenceManager
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.CoordinatorLayout
import android.support.v4.view.ViewCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.Toast
import org.mozilla.focus.R
import org.mozilla.focus.fragment.PanelFragment
import org.mozilla.focus.navigation.ScreenNavigator
import org.mozilla.rocket.bhaskar.ItemPojo
import org.mozilla.rocket.content.ContentPortalViewState.isLastSessionContent

object ContentPortalViewState {
    @JvmStatic
    var isLastSessionContent = false
    var lasPos = 0
    var lasPosOffset = 0
}

class ContentPortalView : CoordinatorLayout, ContentAdapter.ContentPanelListener {

    var loadMoreListener: LoadMoreListener? = null

    private var recyclerView: RecyclerView? = null
    private var emptyView: View? = null
    private var adapter: ContentAdapter? = null
    private var bottomSheet: View? = null
    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null

    companion object {
        // FIXME: for some reason onApplyWindowInsets  is not called from BrowserFragment -> HomeFragment
        // I guess some view in BrowserFragment has consumed that WindowsInsets. But I can't find it now.
        // This must be fixed before merge
        var cachedPaddingTop = 0
    }

    interface LoadMoreListener {
        fun loadMore()
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
//        setPadding(0, cachedPaddingTop, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    )

    init {
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            cachedPaddingTop = insets?.systemWindowInsetTop ?: 0
            //setPadding(0, cachedPaddingTop, 0, 0)
            insets
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        init()
        if (isLastSessionContent) {
            show(false)
        }
        // reset default value
        isLastSessionContent = false
    }

    private fun init() {

        setupBottomSheet()

        setupData()
    }

    private var linearLayoutManager: LinearLayoutManager? = null

    private fun setupData() {
        this.setOnClickListener { hide() }
        findViewById<Button>(R.id.news_try_again)?.setOnClickListener {
            loadMoreListener?.loadMore()
        }
        recyclerView = findViewById(R.id.recyclerview)
//        val animator = recyclerView?.itemAnimator
//        if (animator is SimpleItemAnimator) {
//            animator.supportsChangeAnimations = false
//        }
        emptyView = findViewById(R.id.empty_view_container)
        adapter = ContentAdapter(this)
        adapter?.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                if (itemCount == 0) {
                    onStatus(PanelFragment.VIEW_TYPE_EMPTY)
                } else {
                    onStatus(PanelFragment.VIEW_TYPE_NON_EMPTY)
//                    recyclerView?.scrollToPosition(ContentPortalViewState.lasPos)
                    linearLayoutManager?.scrollToPositionWithOffset(ContentPortalViewState.lasPos,ContentPortalViewState.lasPosOffset)
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
                    if (visibleItemCount + lastVisibleItem + 10 >= totalItemCount) {
                        animateLoading()
                        loadMoreListener?.loadMore()
                    }
                }
            })
        }

        onStatus(PanelFragment.VIEW_TYPE_EMPTY)
    }

    private fun animateLoading() {
//        news_item_loading.visibility = View.VISIBLE
//        news_item_loading.animate()
    }

    private fun stopLoading() {
//        news_item_loading.visibility = View.GONE
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

        val anim = AnimationUtils.loadAnimation(context, R.anim.tab_transition_fade_in)
        anim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
            }

            override fun onAnimationRepeat(animation: Animation?) {
            }

            override fun onAnimationEnd(animation: Animation?) {
                showInternal()
            }
        })
        this.startAnimation(anim)
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
                // need to set it to expanded, otherwise next time will be closed.
//                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
//                bottomSheetBehavior?.peekHeight = 0
                visibility = GONE
            }
        })

        this.startAnimation(anim)
        return true
    }

    private fun setupBottomSheet() {
        bottomSheet = findViewById(R.id.bottom_sheet)
        bottomSheetBehavior = BottomSheetBehavior.from<View>(bottomSheet)
//        bottomSheetBehavior?.peekHeight = 0
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
            PanelFragment.VIEW_TYPE_EMPTY -> {
                recyclerView?.visibility = View.GONE
                emptyView?.visibility = View.VISIBLE
            }
            PanelFragment.VIEW_TYPE_NON_EMPTY -> {
                recyclerView?.visibility = View.VISIBLE
                emptyView?.visibility = View.GONE
            }
            PanelFragment.ON_OPENING -> {
            }
            else -> {
                recyclerView?.visibility = View.GONE
                emptyView?.visibility = View.GONE
            }
        }
    }

    override fun onItemClicked(url: String) {
        ScreenNavigator.get(context).showBrowserScreen(url, true, false)
        ContentPortalViewState.isLastSessionContent = true

        linearLayoutManager?.findFirstCompletelyVisibleItemPosition()?.let {
            ContentPortalViewState.lasPosOffset = it
        }
    }

    override fun onItemDeleted(item: ItemPojo?) {
        Toast.makeText(context, "I don't delete stuff", Toast.LENGTH_SHORT).show()
    }

    override fun onItemEdited(item: ItemPojo?) {
        Toast.makeText(context, "I don't edit stuff", Toast.LENGTH_SHORT).show()
    }

    fun setData(items: MutableList<ItemPojo>?) {
        stopLoading()
        bottomSheetBehavior?.skipCollapsed = items == null || items.size == 0
        adapter?.submitList(items)
    }
}

private const val PREF_KEY_STRING_NEWS = "pref_key_string_news"

fun isEnable(context: Context?): Boolean {
    return context?.let {
        PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(PREF_KEY_STRING_NEWS, false)
    } ?: false
}