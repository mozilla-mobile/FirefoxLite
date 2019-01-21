/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.content

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.CoordinatorLayout
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import kotlinx.android.synthetic.main.content_portal.view.*
import org.mozilla.focus.R
import org.mozilla.focus.bookmark.BookmarkAdapter
import org.mozilla.focus.fragment.PanelFragment
import org.mozilla.focus.navigation.ScreenNavigator
import org.mozilla.focus.persistence.BookmarkModel
import org.mozilla.focus.persistence.BookmarksDatabase
import org.mozilla.focus.repository.BookmarkRepository
import org.mozilla.focus.viewmodel.BookmarkViewModel
import org.mozilla.rocket.content.ContentPortalViewState.isLastSessionContent

object ContentPortalViewState {
    @JvmStatic
    var isLastSessionContent = false
}

class ContentPortalView : CoordinatorLayout, BookmarkAdapter.BookmarkPanelListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var adapter: BookmarkAdapter
    private lateinit var viewModel: BookmarkViewModel
    private lateinit var bottomSheet: View
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        init()
        if (isLastSessionContent) {
            show()
        }
        // reset default value
        isLastSessionContent = false
    }

    private fun init() {

        setupBottomSheet()

        setupData()

        this.setOnApplyWindowInsetsListener { _, insets ->
            content_panel.setPadding(0, insets?.systemWindowInsetTop ?: 0, 0, 0)
            insets
        }
    }

    private fun setupData() {
        recyclerView = findViewById(R.id.recyclerview)
        emptyView = findViewById(R.id.empty_view_container)
        val repository = BookmarkRepository.getInstance(BookmarksDatabase.getInstance(context))
        val factory = BookmarkViewModel.Factory(repository)

        val layoutManager = LinearLayoutManager(context)
        adapter = BookmarkAdapter(this)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = layoutManager

        //https://stackoverflow.com/questions/35685681/dynamically-change-height-of-bottomsheetbehavior
        viewModel = ViewModelProviders.of(context as FragmentActivity, factory).get(BookmarkViewModel::class.java)
        viewModel.bookmarks.observe(context as FragmentActivity, Observer<List<BookmarkModel>> { bms ->
            // why run?
            run {
                if (bms != null && bms.isNotEmpty()) {
                    bottomSheetBehavior.peekHeight = 300 * 3 // fix later
                } else {
                    bottomSheetBehavior.peekHeight = 0 // fix later
                    bottomSheetBehavior.skipCollapsed = true
                }
                bottomSheet.requestLayout()
                adapter.setData(bms)
            }
        })
        onStatus(PanelFragment.VIEW_TYPE_NON_EMPTY)
    }

    fun show() {
        if (visibility == VISIBLE) {
            return
        }
        visibility = VISIBLE
        // bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
//        val anim = AnimationUtils.loadAnimation(context, R.anim.tab_transition_fade_in)
//        anim.setAnimationListener(object : Animation.AnimationListener {
//            override fun onAnimationStart(animation: Animation?) {
//            }
//
//            override fun onAnimationRepeat(animation: Animation?) {
//            }
//
//            override fun onAnimationEnd(animation: Animation?) {
//                visibility = VISIBLE
//                if (forceExpand) {
//                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
//                }
//            }
//        })
//        this.startAnimation(anim)
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
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                visibility = GONE
            }
        })

        this.startAnimation(anim)
        return true
    }

    private fun setupBottomSheet() {
        bottomSheet = findViewById<View>(R.id.bottom_sheet)
        bottomSheetBehavior = BottomSheetBehavior.from<View>(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        bottomSheetBehavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {

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
                recyclerView.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
            }
            PanelFragment.VIEW_TYPE_NON_EMPTY -> {
                recyclerView.visibility = View.VISIBLE
                emptyView.visibility = View.GONE
            }
            PanelFragment.ON_OPENING -> {
            }
            else -> {
                recyclerView.visibility = View.GONE
                emptyView.visibility = View.GONE
            }
        }
    }

    override fun onItemClicked(url: String?) {
        ScreenNavigator.get(context).showBrowserScreen(url, true, false)
        ContentPortalViewState.isLastSessionContent = true
    }

    override fun onItemDeleted(bookmark: BookmarkModel?) {
        Toast.makeText(context, "I don't delete stuff", Toast.LENGTH_SHORT).show()
    }

    override fun onItemEdited(bookmark: BookmarkModel?) {
        Toast.makeText(context, "I don't edit stuff", Toast.LENGTH_SHORT).show()
    }
}
