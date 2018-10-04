/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.rocket.privately.home

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.webkit.ValueCallback
import android.webkit.WebChromeClient.FileChooserParams
import com.airbnb.lottie.LottieAnimationView
import org.mozilla.focus.R
import org.mozilla.focus.locale.LocaleAwareFragment
import org.mozilla.focus.navigation.ScreenNavigator
import org.mozilla.focus.tabs.TabCounter
import org.mozilla.focus.widget.FragmentListener
import org.mozilla.focus.widget.FragmentListener.TYPE.SHOW_TAB_TRAY
import org.mozilla.focus.widget.FragmentListener.TYPE.SHOW_URL_INPUT
import org.mozilla.rocket.privately.SharedViewModel
import org.mozilla.rocket.tabs.SessionManager
import org.mozilla.rocket.tabs.TabViewEngineSession
import org.mozilla.rocket.tabs.TabsSessionProvider

class PrivateHomeFragment : LocaleAwareFragment(),
        ScreenNavigator.HomeScreen {

    private lateinit var sessionManager: SessionManager
    private lateinit var managerObserver: SessionManagerObserver
    private lateinit var logoMan: LottieAnimationView
    private lateinit var fakeInput: View
    private lateinit var tabCounter: TabCounter

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        managerObserver = SessionManagerObserver(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View? {
        sessionManager = TabsSessionProvider.getOrThrow(activity)
        sessionManager.register(managerObserver)

        val view = inflater.inflate(R.layout.fragment_private_homescreen, container, false)
        logoMan = view.findViewById(R.id.pm_home_logo)
        fakeInput = view.findViewById(R.id.pm_home_fake_input)
        tabCounter = view.findViewById(R.id.btn_tab_tray)

        ClickListener(this).let {
            fakeInput.setOnClickListener(it)
            tabCounter.setOnClickListener(it)
        }

        observeViewModel()
        updateTabCounter(sessionManager.tabsCount)

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        sessionManager.unregister(managerObserver)
    }

    override fun getFragment(): Fragment {
        return this
    }

    private fun observeViewModel() {
        activity?.apply {
            // since the view model is of the activity, use the fragment's activity instead of the fragment itself
            ViewModelProviders.of(this).get(SharedViewModel::class.java)
                    .urlInputState()
                    .observe(this, Observer<Boolean> {
                        it?.apply {
                            onUrlInputScreenVisible(it)
                        }
                    })
        }
    }

    override fun applyLocale() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    companion object {
        fun create(): PrivateHomeFragment {
            return PrivateHomeFragment()
        }
    }

    private fun animatePrivateHome() {
        logoMan.playAnimation()
    }

    override fun onUrlInputScreenVisible(visible: Boolean) {
        if (visible) {
            logoMan.visibility = View.INVISIBLE
            fakeInput.visibility = View.INVISIBLE
        } else {
            logoMan.visibility = View.VISIBLE
            fakeInput.visibility = View.VISIBLE
        }
    }

    override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
        if (enter) {
            val anim = AnimationUtils.loadAnimation(activity, R.anim.pb_enter)

            anim.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                }

                override fun onAnimationRepeat(animation: Animation?) {
                }

                override fun onAnimationEnd(animation: Animation?) {
                    animatePrivateHome()
                }
            })

            return anim
        } else {
            return super.onCreateAnimation(transit, enter, nextAnim)
        }
    }

    fun updateTabCounter(count: Int) {
        tabCounter.setCount(count)
    }

    class ClickListener(val fragment: Fragment) : View.OnClickListener {
        val parent: FragmentListener = if (fragment.activity is FragmentListener)
            fragment.activity as FragmentListener
        else throw RuntimeException("")

        override fun onClick(v: View?) {
            when (v?.id) {
                R.id.pm_home_fake_input -> parent.onNotified(fragment, SHOW_URL_INPUT, null)
                R.id.btn_tab_tray -> parent.onNotified(fragment, SHOW_TAB_TRAY, null)
            }
        }
    }

    class SessionManagerObserver(private val hostFragment: PrivateHomeFragment) : SessionManager.Observer {
        override fun updateFailingUrl(url: String?, updateFromError: Boolean) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun handleExternalUrl(url: String?): Boolean {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onShowFileChooser(es: TabViewEngineSession, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?): Boolean {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onSessionCountChanged(count: Int) {
            super.onSessionCountChanged(count)
            hostFragment.updateTabCounter(count)
        }
    }
}
