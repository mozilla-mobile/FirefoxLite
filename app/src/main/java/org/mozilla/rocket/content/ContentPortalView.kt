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
import android.support.v4.widget.NestedScrollView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import kotlinx.android.synthetic.main.content_portal.view.*
import org.mozilla.focus.R
import org.mozilla.focus.fragment.PanelFragment
import org.mozilla.focus.navigation.ScreenNavigator
import org.mozilla.rocket.bhaskar.ItemPojo
import org.mozilla.rocket.content.ContentPortalViewState.isLastSessionContent

object ContentPortalViewState {
    @JvmStatic
    var isLastSessionContent = false
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

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    )

    init {
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            cachedPaddingTop = insets?.systemWindowInsetTop ?: 0
//            setPadding(0, cachedPaddingTop, 0, 0)
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

    private fun setupData() {
        this.setOnClickListener { hide() }
        findViewById<View>(R.id.news_try_again)?.setOnClickListener {
            loadMoreListener?.loadMore()
        }
        recyclerView = findViewById(R.id.recyclerview)
        val animator = recyclerView?.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }
        emptyView = findViewById(R.id.empty_view_container)
        adapter = ContentAdapter(this)
        recyclerView?.adapter = adapter
        recyclerView?.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        findViewById<NestedScrollView>(R.id.news_main)?.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, oldScrollY ->
                val pageSize = v.measuredHeight
                // v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight() - scrollY is -49dp
                // When scrolled to end due to padding
                if (scrollY > oldScrollY && v.getChildAt(0).measuredHeight - v.measuredHeight - scrollY < pageSize) {
                    animateLoading()
                    loadMoreListener?.loadMore()
                }
            })
        onStatus(PanelFragment.VIEW_TYPE_NON_EMPTY)
    }

    private fun animateLoading() {
        news_item_loading.visibility = View.VISIBLE
        news_item_loading.animate()
    }

    private fun stopLoading() {
        news_item_loading.visibility = View.GONE
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
                visibility = GONE
            }
        })

        this.startAnimation(anim)
        return true
    }

    private fun setupBottomSheet() {
        bottomSheet = findViewById(R.id.bottom_sheet)
        bottomSheetBehavior = BottomSheetBehavior.from<View>(bottomSheet)
//        bottomSheetBehavior.isFitToContents
//        bottomSheetBehavior?.isFitToContents = false
//        bottomSheetBehavior?.peekHeight = 0
//        bottomSheetBehavior?.skipCollapsed = true
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
    }

    override fun onItemDeleted(item: ItemPojo?) {
        Toast.makeText(context, "I don't delete stuff", Toast.LENGTH_SHORT).show()
    }

    override fun onItemEdited(item: ItemPojo?) {
        Toast.makeText(context, "I don't edit stuff", Toast.LENGTH_SHORT).show()
    }

    fun setData(items: List<ItemPojo>?) {
        stopLoading()
//        bottomSheetBehavior?.skipCollapsed = items == null || items.size == 0

//
//        val start: Int = adapter?.items?.size ?: 0
////        val end = items?.size ?: 0
//        adapter?.items = items
//        adapter?.notifyItemRangeInserted(start, 20)
//        adapter?.notifyDataSetChanged()
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