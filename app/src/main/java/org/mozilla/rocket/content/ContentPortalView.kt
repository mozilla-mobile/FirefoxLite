/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.content

import android.content.Context
import android.support.design.widget.CoordinatorLayout
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentTransaction
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import org.mozilla.focus.R
import org.mozilla.focus.navigation.ScreenNavigator
import org.mozilla.focus.utils.AppConfigWrapper
import org.mozilla.lite.partner.NewsItem
import org.mozilla.rocket.content.data.ShoppingLink
import org.mozilla.rocket.content.view.ContentFragment
import org.mozilla.rocket.widget.BottomSheetBehavior

interface ContentPortalListener {
    fun onItemClicked(url: String)
    fun onStatus(items: MutableList<out NewsItem>?)
}

const val TYPE_NEWS = 1 shl 0
const val TYPE_TICKET = 1 shl 1
const val TYPE_COUPON = 1 shl 2
const val TYPE_KEY = "contentType"

class ContentPortalView : CoordinatorLayout, ContentPortalListener {

    // shared views for News and E-Commerce
    private var recyclerView: RecyclerView? = null
    private var bottomSheet: LinearLayout? = null
    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null

    // views for News
    var newsListListener: NewsListListener? = null
    private var newsEmptyView: View? = null
    private var newsProgressCenter: ProgressBar? = null
    private var newsAdapter: NewsAdapter<NewsItem>? = null
    private var newsListLayoutManager: LinearLayoutManager? = null

    interface NewsListListener {
        fun loadMore()
        fun onShow(context: Context)
    }

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
        val features = features()
        if (features.size == 1 || AppConfigWrapper.hasNewsPortal()) {
            initLegacyContentPortal()
        } else {
            initContentFragment(features)
        }
    }

    // get the features from remote config
    private fun features(): ArrayList<Int> {

        val features = ArrayList<Int>()
        if (AppConfigWrapper.hasEcommerceCoupons()) {
            features.add(TYPE_COUPON)
        }

        if (AppConfigWrapper.hasEcommerceShoppingLink()) {
            features.add(TYPE_TICKET)
        }

        if (AppConfigWrapper.hasNewsPortal()) {
             features.add(TYPE_NEWS) // we don't support News in Tab now.
        }

        return features
    }

    private fun initContentFragment(features: ArrayList<Int>) {
        val adjusted = adjust(features)
        context?.inTransaction {
            replace(R.id.bottom_sheet, ContentFragment.newInstance(adjusted), TAG_CONTENT_FRAGMENT)
        }
    }

    // we don't support News in ContentFragment yet
    private fun adjust(features: ArrayList<Int>): ArrayList<Int> {
        val list = ArrayList<Int>()
        for (feature in features) {
            if (feature != TYPE_NEWS) {
                list.add(feature)
            }
        }
        return list
    }

    private fun initLegacyContentPortal() {
        when {
            AppConfigWrapper.hasEcommerceCoupons() -> setupViewCouponList()
            AppConfigWrapper.hasEcommerceShoppingLink() -> setupViewShoppingLink()
            else -> setupViewNews()
        }
    }

    private fun setupViewCouponList() {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.content_coupon, bottomSheet)

        val recyclerView = findViewById<RecyclerView>(R.id.content_coupons)
        val couponAdapter = CouponAdapter(this)
        recyclerView?.layoutManager = LinearLayoutManager(context)
        recyclerView?.adapter = couponAdapter

        val coupons = AppConfigWrapper.getEcommerceCoupons()
        couponAdapter.submitList(coupons)
    }

    private fun setupViewShoppingLink() {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.content_shoppinglink, bottomSheet)

        recyclerView = findViewById(R.id.ct_shoppinglink_list)
        val shoppinglinkAdapter = ShoppingLinkAdapter(this)
        recyclerView?.layoutManager = GridLayoutManager(context, SHOPPINGLINK_GRID_SPAN)
        recyclerView?.adapter = shoppinglinkAdapter

        val ecommerceShoppingLinks = AppConfigWrapper.getEcommerceShoppingLinks()
        ecommerceShoppingLinks.add(ShoppingLink("", "footer", "", ""))
        shoppinglinkAdapter.submitList(ecommerceShoppingLinks)
    }

    private fun setupViewNews() {

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.content_news, bottomSheet)

        findViewById<Button>(R.id.news_try_again)?.setOnClickListener {
            newsListListener?.loadMore()
        }
        recyclerView = findViewById(R.id.news_list)
        newsEmptyView = findViewById(R.id.empty_view_container)
        newsProgressCenter = findViewById(R.id.news_progress_center)
        newsAdapter = NewsAdapter(this)

        recyclerView?.adapter = newsAdapter
        newsListLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        recyclerView?.layoutManager = newsListLayoutManager
        newsListLayoutManager?.let {
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

    private fun showInternal() {
        visibility = VISIBLE
        // TODO: if previously is collapsed, collapse here.
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    // helper method to work with FragmentManager
    private inline fun Context.inTransaction(func: FragmentTransaction.() -> FragmentTransaction) {
        ( this as? FragmentActivity)?.supportFragmentManager?.beginTransaction()?.func()?.commit()
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

    /**
     * Hide content portal view.
     */
    fun hide(): Boolean {
        if (visibility == GONE) {
            return false
        }

        HomeFragmentViewState.reset()
        NewsRepository.reset()
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

    override fun onStatus(items: MutableList<out NewsItem>?) {
        when {
            items == null -> {
                recyclerView?.visibility = View.GONE
                newsEmptyView?.visibility = View.GONE
                newsProgressCenter?.visibility = View.VISIBLE
                bottomSheetBehavior?.skipCollapsed = true
            }
            items.size == 0 -> {
                recyclerView?.visibility = View.GONE
                newsEmptyView?.visibility = View.VISIBLE
                newsProgressCenter?.visibility = View.GONE
                bottomSheetBehavior?.skipCollapsed = true
            }
            else -> {
                recyclerView?.visibility = View.VISIBLE
                newsEmptyView?.visibility = View.GONE
                newsProgressCenter?.visibility = View.GONE
            }
        }
    }

    override fun onItemClicked(url: String) {
        ScreenNavigator.get(context).showBrowserScreen(url, true, false)

        // use findFirstVisibleItemPosition so we don't need to remember offset
        newsListLayoutManager?.findFirstVisibleItemPosition()?.let {
            HomeFragmentViewState.lastScrollPos = it
        }
    }

    /**
     * Update news content on content portal view
     * */
    fun setNewsContent(items: MutableList<out NewsItem>?) {

        onStatus(items)

        newsAdapter?.submitList(items)
        HomeFragmentViewState.lastScrollPos?.let {
            val size = items?.size
            if (size != null && size > it) {
                newsListLayoutManager?.scrollToPosition(it)
                // forget about last scroll position
                HomeFragmentViewState.lastScrollPos = null
            }
        }
    }

    /**
     * Check if content portal view need to show or hide itself base on previous sate
     * */
    fun onResume() {
        if (HomeFragmentViewState.isLastOpenNews()) {
            show(false)
        } else {
            hide()
        }
    }

    companion object {
        private const val NEWS_THRESHOLD = 10
        private const val SHOPPINGLINK_GRID_SPAN = 2
        private const val TAG_CONTENT_FRAGMENT = "TAG_CONTENT_FRAGMENT"
    }
}
