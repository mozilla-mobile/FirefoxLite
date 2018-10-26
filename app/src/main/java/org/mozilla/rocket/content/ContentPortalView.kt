/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.content

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.CoordinatorLayout
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.View
import android.widget.Toast

import org.mozilla.focus.R
import org.mozilla.focus.bookmark.BookmarkAdapter
import org.mozilla.focus.fragment.PanelFragment
import org.mozilla.focus.navigation.ScreenNavigator
import org.mozilla.focus.persistence.BookmarkModel
import org.mozilla.focus.persistence.BookmarksDatabase
import org.mozilla.focus.repository.BookmarkRepository
import org.mozilla.focus.viewmodel.BookmarkViewModel

class ContentPortalView : CoordinatorLayout, BookmarkAdapter.BookmarkPanelListener {
    override fun onItemClicked(url: String?) {
        ScreenNavigator.get(context).showBrowserScreen(url, true, false)
    }

    override fun onLayoutChild(child: View, layoutDirection: Int) {
        super.onLayoutChild(child, layoutDirection)
    }

    override fun onItemDeleted(bookmark: BookmarkModel?) {
        Toast.makeText(context, "I don't delete stuff", Toast.LENGTH_SHORT).show()
    }

    override fun onItemEdited(bookmark: BookmarkModel?) {
        Toast.makeText(context, "I don't edit stuff", Toast.LENGTH_SHORT).show()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        init()
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
            else -> {
                recyclerView.visibility = View.GONE
                emptyView.visibility = View.GONE
            }
        }
    }


    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var adapter: BookmarkAdapter
    private lateinit var viewModel: BookmarkViewModel
    private lateinit var bottomsheet: View
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>


    var listener: PortalListener? = null

    interface PortalListener {
        fun onHidden()
        fun onShown()
    }


    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)


    private var systemBarHeight = 0

    private fun init() {

        setupBottomSheet()

        recyclerView = findViewById(R.id.recyclerview)
        emptyView = findViewById(R.id.empty_view_container)
        val factory = BookmarkViewModel.Factory(
                BookmarkRepository.getInstance(BookmarksDatabase.getInstance(context)))

        val layoutManager = LinearLayoutManager(context)
        adapter = BookmarkAdapter(this)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = layoutManager

        //https://stackoverflow.com/questions/35685681/dynamically-change-height-of-bottomsheetbehavior
        viewModel = ViewModelProviders.of(context as FragmentActivity, factory)
                .get(BookmarkViewModel::class.java)
        viewModel.bookmarks.observe(context as FragmentActivity, Observer<List<BookmarkModel>> { bms ->
            // why run?
            run {
                if (bms != null && bms.isNotEmpty()) {
                    bottomSheetBehavior.peekHeight = 300 * 3 // fix later
                } else {
                    bottomSheetBehavior.peekHeight = 0 // fix later
                }
                bottomsheet.requestLayout()
                adapter.setData(bms)
            }
        })
        onStatus(PanelFragment.VIEW_TYPE_NON_EMPTY)

        this.setOnApplyWindowInsetsListener { _, insets ->
            systemBarHeight = insets?.systemWindowInsetTop ?: 0

            insets
        }
    }

    fun moveUp() {
        val c = ConstraintSet()
        c.clone(context, R.layout.fragment_homescreen)
        val host = parent as ConstraintLayout
        TransitionManager.beginDelayedTransition(host)
        c.connect(R.id.content_panel, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 63)
        c.applyTo(host)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun moveDown() {
        val c = ConstraintSet()
        c.clone(context, R.layout.fragment_homescreen)
        val host = parent as ConstraintLayout
        TransitionManager.beginDelayedTransition(host)
        c.clear(R.id.content_panel, ConstraintSet.TOP)
        c.applyTo(host)
    }


    private fun setupBottomSheet() {
        bottomsheet = findViewById<View>(R.id.bottom_sheet)

        bottomSheetBehavior = BottomSheetBehavior.from<View>(bottomsheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheetBehavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    listener?.onHidden()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // we don't want to slide other stuff here
            }
        })
    }
}
