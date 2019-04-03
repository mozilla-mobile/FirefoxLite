/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.content

import android.content.Context
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.CoordinatorLayout
import android.support.v7.widget.GridLayoutManager
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
import android.view.LayoutInflater
import android.widget.LinearLayout
import org.mozilla.rocket.content.data.Ticket

class ContentPortalView : CoordinatorLayout, ContentAdapter.ContentPanelListener {

    var newsListListener: NewsListListener? = null

    private var recyclerView: RecyclerView? = null
    private var emptyView: View? = null
    private var progressCenter: ProgressBar? = null
    private var adapter: ContentAdapter<NewsItem>? = null
    private var bottomSheet: LinearLayout? = null
    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null

    private var contentType: ContentType = ContentType.News

    interface NewsListListener {
        fun loadMore()
        fun onShow(context: Context)
    }

    sealed class ContentType {
        object News : ContentType()
        object Ticket : ContentType()
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

        when(contentType){
            ContentType.News -> setupViewNews()
            ContentType.Ticket -> setupViewTicket()
        }

    }

    private fun setupViewTicket() {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.content_ticket, bottomSheet)

        recyclerView = findViewById(R.id.ct_ticket_list)
        val ticketAdapter = TicketAdapter(this)
        recyclerView?.layoutManager = GridLayoutManager(context, TICKET_GRID_SPAN)
        recyclerView?.adapter = ticketAdapter

        ticketAdapter.submitList(getTickets())

    }

    private var linearLayoutManager: LinearLayoutManager? = null

    private fun setupViewNews() {

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.content_news, bottomSheet)

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

    private fun getTickets() = ArrayList<Ticket>().apply {
        add(
            Ticket(
                "http://bukalapak.go2cloud.org/aff_c?offer_id=15&aff_id=4287&url=https%3A%2F%2Fwww.bukalapak.com%2Fc%2Ftiket-voucher%2Ftiket-voucher-lainnya%3Fho_offer_id%3D{offer_id}%26ho_trx_id%3D{transaction_id}%26affiliate_id%3D{affiliate_id}%26utm_source%3Dhasoffers%26utm_medium%3Daffiliate%26utm_campaign%3D{offer_id}%26ref%3D{referer}",
                "Pulsa",
                R.drawable.image_pulsa
            )
        )

        add(
            Ticket(
                "http://bukalapak.go2cloud.org/aff_c?offer_id=15&aff_id=4287&url=https%3A%2F%2Fwww.bukalapak.com%2Fc%2Ftiket-voucher%2Ftiket-voucher-lainnya%3Fho_offer_id%3D{offer_id}%26ho_trx_id%3D{transaction_id}%26affiliate_id%3D{affiliate_id}%26utm_source%3Dhasoffers%26utm_medium%3Daffiliate%26utm_campaign%3D{offer_id}%26ref%3D{referer}",
                "Flight Ticket",
                R.drawable.image_flight
            )
        )

        add(
            Ticket(
                "http://bukalapak.go2cloud.org/aff_c?offer_id=15&aff_id=4287&url=https%3A%2F%2Fwww.bukalapak.com%2Fc%2Ftiket-voucher%2Ftiket-voucher-lainnya%3Fho_offer_id%3D{offer_id}%26ho_trx_id%3D{transaction_id}%26affiliate_id%3D{affiliate_id}%26utm_source%3Dhasoffers%26utm_medium%3Daffiliate%26utm_campaign%3D{offer_id}%26ref%3D{referer}",
                "Event",
                R.drawable.image_event
            )
        )

        add(
            Ticket(
                "http://bukalapak.go2cloud.org/aff_c?offer_id=15&aff_id=4287&url=https%3A%2F%2Fwww.bukalapak.com%2Fc%2Ftiket-voucher%2Ftiket-voucher-lainnya%3Fho_offer_id%3D{offer_id}%26ho_trx_id%3D{transaction_id}%26affiliate_id%3D{affiliate_id}%26utm_source%3Dhasoffers%26utm_medium%3Daffiliate%26utm_campaign%3D{offer_id}%26ref%3D{referer}",
                "Data Package",
                R.drawable.image_data_package
            )
        )

        add(
            Ticket(
                "http://bukalapak.go2cloud.org/aff_c?offer_id=15&aff_id=4287&url=https%3A%2F%2Fwww.bukalapak.com%2Fc%2Ftiket-voucher%2Ftiket-voucher-lainnya%3Fho_offer_id%3D{offer_id}%26ho_trx_id%3D{transaction_id}%26affiliate_id%3D{affiliate_id}%26utm_source%3Dhasoffers%26utm_medium%3Daffiliate%26utm_campaign%3D{offer_id}%26ref%3D{referer}",
                "Game",
                R.drawable.image_game
            )
        )

        add(
            Ticket(
                "http://bukalapak.go2cloud.org/aff_c?offer_id=15&aff_id=4287&url=https%3A%2F%2Fwww.bukalapak.com%2Fc%2Ftiket-voucher%2Ftiket-voucher-lainnya%3Fho_offer_id%3D{offer_id}%26ho_trx_id%3D{transaction_id}%26affiliate_id%3D{affiliate_id}%26utm_source%3Dhasoffers%26utm_medium%3Daffiliate%26utm_campaign%3D{offer_id}%26ref%3D{referer}",
                "Train",
                R.drawable.image_train
            )
        )

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
        private const val TICKET_GRID_SPAN = 2
    }
}