/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.privately

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.annotation.CheckResult
import android.support.v4.app.Fragment
import android.view.View
import android.widget.Toast
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import org.mozilla.focus.R
import org.mozilla.focus.activity.MainActivity
import org.mozilla.focus.locale.LocaleAwareAppCompatActivity
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.urlinput.UrlInputFragment
import org.mozilla.focus.widget.BackKeyHandleable
import org.mozilla.focus.widget.FragmentListener
import org.mozilla.focus.widget.FragmentListener.TYPE
import org.mozilla.rocket.component.PrivateSessionNotificationService
import org.mozilla.rocket.tabs.TabsSession
import org.mozilla.rocket.tabs.TabsSessionProvider

class PrivateModeActivity : LocaleAwareAppCompatActivity(),
        FragmentListener,
        TabsSessionProvider.SessionHost {

    private var session: TabsSession? = null
    private lateinit var tabViewProvider: PrivateTabViewProvider
    private lateinit var sharedViewModel: SharedViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tabViewProvider = PrivateTabViewProvider(this)

        val exitEarly = handleIntent(intent)
        if (exitEarly) {
            pushToBack()
            return
        }

        setContentView(R.layout.activity_private_mode)

        makeStatusBarTransparent()

        initViewModel()
    }

    private fun initViewModel() {
        sharedViewModel = ViewModelProviders.of(this).get(SharedViewModel::class.java)
        sharedViewModel.urlInputState().value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPrivateMode()
        session?.destroy()
    }

    @Override
    override fun onSupportNavigateUp() = getNavController().navigateUp()

    override fun applyLocale() {}

    override fun onNotified(from: Fragment, type: FragmentListener.TYPE, payload: Any?) {
        when (type) {
            TYPE.TOGGLE_PRIVATE_MODE -> pushToBack()
            TYPE.SHOW_URL_INPUT -> showUrlInput(payload)
            TYPE.DISMISS_URL_INPUT -> dismissUrlInput()
            TYPE.OPEN_URL_IN_CURRENT_TAB -> openUrl(payload)
            TYPE.OPEN_URL_IN_NEW_TAB -> openUrl(payload)
            TYPE.DROP_BROWSING_PAGES -> dropBrowserFragment()
            else -> {
            }
        }
    }

    override fun onBackPressed() {
        val cnt = supportFragmentManager.backStackEntryCount
        if (cnt != 0) {
            supportFragmentManager.popBackStack()
            // normally this means we are hiding the keyboard
            sharedViewModel.urlInputState().value = false

        } else {
            val controller = getNavController()
            if (controller.currentDestination.id == R.id.fragment_private_home_screen) {
                super.onBackPressed()
            } else {
                // To find the last added fragment, and to give a opportunity of handling back-key.
                // XXX: The way to find target fragment might be unstable, but this is all we have now.
                val host = getNavHost()
                val lastAddedFragment = host.childFragmentManager.findFragmentById(R.id.private_nav_host_fragment)
                if (lastAddedFragment is BackKeyHandleable) {
                    if (lastAddedFragment.onBackPressed()) {
                        return
                    }
                }
                dropBrowserFragment()
            }
        }
    }

    override fun getTabsSession(): TabsSession {
        if (session == null) {
            session = TabsSession(tabViewProvider)
        }

        // we just created it, it definitely not null
        return session!!
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        val exitEarly = handleIntent(intent)
        if (exitEarly) {
            return
        }
    }

    private fun dropBrowserFragment() {
        getNavController().navigateUp()
        stopPrivateMode()
    }

    private fun pushToBack() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(intent)
        overridePendingTransition(0, R.anim.pb_exit)
    }

    private fun showUrlInput(payload: Any?) {
        val url = payload?.toString() ?: ""

        var frgMgr = supportFragmentManager

        if (isUrlInputDisplaying()) {
            // We are already showing an URL input fragment. This might have been a double click on the
            // fake URL bar. Just ignore it.
            return
        }
        val urlFragment: UrlInputFragment = UrlInputFragment.create(url, null, false)
        val transaction = frgMgr.beginTransaction()
        transaction.add(R.id.container, urlFragment, UrlInputFragment.FRAGMENT_TAG)
                .addToBackStack(UrlInputFragment.FRAGMENT_TAG)
                .commit()

        sharedViewModel.urlInputState().value = true
    }

    private fun dismissUrlInput() {
        if (isUrlInputDisplaying()) {
            supportFragmentManager.popBackStack()
        }
        sharedViewModel.urlInputState().value = false
    }

    private fun openUrl(payload: Any?) {
        val url = payload?.toString() ?: ""

        ViewModelProviders.of(this)
                .get(SharedViewModel::class.java)
                .setUrl(url)

        dismissUrlInput()
        val controller = getNavController()
        if (controller.currentDestination.id == R.id.fragment_private_home_screen) {
            controller.navigate(R.id.action_private_home_to_browser)
        }
        startPrivateMode()
    }

    private fun isUrlInputDisplaying(): Boolean {
        val frg = supportFragmentManager.findFragmentByTag(UrlInputFragment.FRAGMENT_TAG)
        return ((frg != null) && frg.isAdded && !frg.isRemoving)
    }

    private fun getNavHost() = supportFragmentManager.findFragmentById(R.id.private_nav_host_fragment) as NavHostFragment

    private fun getNavController() = Navigation.findNavController(this, R.id.private_nav_host_fragment)

    private fun makeStatusBarTransparent() {
        var visibility = window.decorView.systemUiVisibility
        // do not overwrite existing value
        visibility = visibility or (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        window.decorView.systemUiVisibility = visibility
    }

    private fun startPrivateMode() {
        PrivateSessionNotificationService.start(this)
    }


    private fun stopPrivateMode() {
        PrivateSessionNotificationService.stop(this)
        PrivateMode.sanitize(this.applicationContext)
        tabViewProvider.purify()
        Toast.makeText(this, R.string.private_browsing_erase_done, Toast.LENGTH_LONG).show()
    }

    @CheckResult
    private fun handleIntent(intent: Intent?): Boolean {

        if (intent?.action == PrivateMode.INTENT_EXTRA_SANITIZE) {
            TelemetryWrapper.erasePrivateModeNotification()
            stopPrivateMode()
            finishAndRemoveTask()
            return true
        }
        return false
    }


}
