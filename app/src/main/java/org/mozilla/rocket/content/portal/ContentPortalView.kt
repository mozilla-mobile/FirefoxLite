/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.content.portal

import android.content.Context
import android.support.design.widget.CoordinatorLayout
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentTransaction
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import org.mozilla.focus.R
import org.mozilla.lite.partner.NewsItem
import org.mozilla.rocket.content.ContentPortalViewState
import org.mozilla.rocket.content.ecommerce.EcTabFragment
import org.mozilla.rocket.content.news.NewsTabFragment
import org.mozilla.rocket.widget.BottomSheetBehavior

interface ContentPortalListener {
    fun onItemClicked(url: String)
    fun onStatus(items: List<NewsItem>?)
}

class ContentPortalView : CoordinatorLayout {

    private var bottomSheet: LinearLayout? = null
    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null

    private val contentFeature = ContentFeature()

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
            context, attrs, defStyleAttr
    )

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // setup common views used for content portal
        setupContentPortalView()

        // if there's only one feature or it's production build, we init the legacy content portal
        if (contentFeature.hasNews()) {
            initNewsFragment()
        } else {
            initEcTabFragment()
        }
    }

    private fun initNewsFragment() {
        context?.inTransaction {
            replace(R.id.bottom_sheet, NewsTabFragment.newInstance(bottomSheetBehavior), TAG_NEWS_FRAGMENT)
        }
    }

    private fun initEcTabFragment() {
        context?.inTransaction {
            replace(R.id.bottom_sheet, EcTabFragment.newInstance(),
                TAG_CONTENT_FRAGMENT
            )
        }
    }

    private fun showInternal() {
        visibility = VISIBLE
        // TODO: if previously is collapsed, collapse here.
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    // helper method to work with FragmentManager
    private inline fun Context.inTransaction(func: FragmentTransaction.() -> FragmentTransaction) {
        (this as? FragmentActivity)?.supportFragmentManager?.beginTransaction()?.func()?.commit()
    }

    /**
     * Display content portal view.
     *
     * @param animated false if we don't want the animation. E.g.  restore the view state
     */
    fun show(animated: Boolean) {
        if (visibility == VISIBLE) {
            return
        }
        ContentPortalViewState.lastOpened()

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

    /**
     * Hide content portal view.
     */
    fun hide(): Boolean {
        if (visibility == GONE) {
            return false
        }

        ContentPortalViewState.reset()
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

    private fun setupContentPortalView() {

        this.setOnClickListener { hide() }

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

    /**
     * Check if content portal view need to show or hide itself base on previous sate
     * */
    fun onResume() {
        if (ContentPortalViewState.isOpened()) {
            show(false)
        } else {
            hide()
        }
    }

    companion object {
        private const val TAG_NEWS_FRAGMENT = "TAG_NEWS_FRAGMENT"
        private const val TAG_CONTENT_FRAGMENT = "TAG_CONTENT_FRAGMENT"
    }
}
