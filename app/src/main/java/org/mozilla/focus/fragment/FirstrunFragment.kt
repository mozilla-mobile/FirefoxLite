/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.mozilla.focus.R
import org.mozilla.focus.activity.MainActivity
import org.mozilla.focus.firstrun.DefaultFirstrunPagerAdapter
import org.mozilla.focus.firstrun.UpgradeFirstrunPagerAdapter
import org.mozilla.focus.navigation.ScreenNavigator.Screen
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.DialogUtils
import org.mozilla.focus.utils.NewFeatureNotice
import org.mozilla.rocket.periodic.FirstLaunchWorker
import org.mozilla.rocket.periodic.PeriodicReceiver

class FirstrunFragment : Fragment(), View.OnClickListener, Screen {
    private lateinit var viewPager: ViewPager

    private lateinit var bgTransitionDrawable: TransitionDrawable
    private lateinit var bgDrawables: Array<Drawable>

    private var isTelemetryValid = true
    private var telemetryStartTimestamp: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initDrawables()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        val transition = TransitionInflater.from(context).inflateTransition(R.transition.firstrun_exit)

        exitTransition = transition

        // We will send a telemetry event whenever a new firstrun page is shown. However this page
        // listener won't fire for the initial page we are showing. So we are going to firing here.
        isTelemetryValid = true
        telemetryStartTimestamp = System.currentTimeMillis()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_firstrun, container, false)
        view.isClickable = true

        view.findViewById<View>(R.id.skip).setOnClickListener(this)

        val background = view.findViewById<View>(R.id.background)
        background.background = bgTransitionDrawable

        val adapter = findPagerAdapter(container!!.context, this)
        if (adapter == null) {
            finishFirstrun()
            return view
        }

        viewPager = view.findViewById<View>(R.id.pager) as ViewPager

        viewPager.setPageTransformer(true) { page, position -> page.alpha = 1 - 0.5f * Math.abs(position) }

        viewPager.clipToPadding = false
        viewPager.adapter = adapter
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {

            override fun onPageSelected(newIdx: Int) {
                val duration = 400
                val nextDrawable = bgDrawables[newIdx % bgDrawables.size]

                if (newIdx % 2 == 0) {
                    // next page is even number
                    bgTransitionDrawable.setDrawableByLayerId(R.id.first_run_bg_even, nextDrawable)
                    bgTransitionDrawable.reverseTransition(duration) // odd -> even
                } else {
                    // next page is odd number
                    bgTransitionDrawable.setDrawableByLayerId(R.id.first_run_bg_odd, nextDrawable)
                    bgTransitionDrawable.startTransition(duration) // even -> odd
                }
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageScrollStateChanged(state: Int) {}
        })

        if (adapter.count > 1) {
            val tabLayout = view.findViewById<View>(R.id.tabs) as TabLayout
            tabLayout.setupWithViewPager(viewPager, true)
        }

        return view
    }

    override fun onPause() {
        super.onPause()
        isTelemetryValid = false
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.next -> viewPager.currentItem = viewPager.currentItem + 1

            R.id.skip -> finishFirstrun()

            R.id.finish -> {
                promoteSetDefaultBrowserIfPreload()
                finishFirstrun()
                if (isTelemetryValid) {
                    TelemetryWrapper.finishFirstRunEvent(System.currentTimeMillis() - telemetryStartTimestamp)
                }
            }

            else -> throw IllegalArgumentException("Unknown view")
        }
    }

    override fun getFragment(): Fragment {
        return this
    }

    private fun isSystemApp() = context?.applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM) != 0

    private fun promoteSetDefaultBrowserIfPreload(): FirstrunFragment {
        // if it's a system app(preload), we'll like to promote set default browser when the user finish first run
        if (isSystemApp()) {
            DialogUtils.showDefaultSettingNotification(context)
        }
        return this
    }

    private fun findPagerAdapter(context: Context, onClickListener: View.OnClickListener): PagerAdapter? {
        val pagerAdapter: PagerAdapter?
        val shown = NewFeatureNotice.getInstance(getContext()).hasShownFirstRun()
        pagerAdapter = if (!shown) {
            DefaultFirstrunPagerAdapter(context, wrapButtonClickListener(onClickListener))
        } else if (NewFeatureNotice.getInstance(getContext()).shouldShowLiteUpdate()) {
            UpgradeFirstrunPagerAdapter(context, onClickListener)
        } else {
            null
        }
        return pagerAdapter
    }

    private fun wrapButtonClickListener(onClickListener: View.OnClickListener): View.OnClickListener {
        return View.OnClickListener { view ->
            if (view.id == R.id.finish) {
                activity?.sendBroadcast(Intent(activity, PeriodicReceiver::class.java).apply {
                    action = FirstLaunchWorker.ACTION
                })
            }
            onClickListener.onClick(view)
        }
    }

    private fun finishFirstrun() {
        NewFeatureNotice.getInstance(context).setFirstRunDidShow()
        NewFeatureNotice.getInstance(context).setLiteUpdateDidShow()

        (activity as MainActivity).firstrunFinished()
    }

    // FirstRun fragment is not used often, so we create drawables programmatically, instead of add
    // lots of drawable resources
    private fun initDrawables() {
        bgDrawables = arrayOf(resources.getDrawable(R.drawable.bg_homescreen_color, context?.theme), resources.getDrawable(R.drawable.bg_homescreen_color, context?.theme), resources.getDrawable(R.drawable.bg_homescreen_color, context?.theme), resources.getDrawable(R.drawable.bg_homescreen_color, context?.theme))

        bgTransitionDrawable = TransitionDrawable(bgDrawables)
        bgTransitionDrawable.setId(0, R.id.first_run_bg_even)
        bgTransitionDrawable.setId(1, R.id.first_run_bg_odd)
    }

    companion object {
        @JvmStatic
        fun create(): FirstrunFragment {
            return FirstrunFragment()
        }
    }
}
