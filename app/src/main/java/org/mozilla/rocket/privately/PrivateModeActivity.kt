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
import org.mozilla.focus.BuildConfig
import org.mozilla.focus.R
import org.mozilla.focus.activity.BaseActivity
import org.mozilla.focus.activity.MainActivity
import org.mozilla.focus.navigation.ScreenNavigator
import org.mozilla.focus.navigation.ScreenNavigator.BrowserScreen
import org.mozilla.focus.navigation.ScreenNavigator.HomeScreen
import org.mozilla.focus.navigation.ScreenNavigator.Screen
import org.mozilla.focus.navigation.ScreenNavigator.UrlInputScreen
import org.mozilla.focus.tabs.tabtray.TabTray
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.urlinput.UrlInputFragment
import org.mozilla.focus.widget.FragmentListener
import org.mozilla.focus.widget.FragmentListener.TYPE
import org.mozilla.rocket.component.PrivateSessionNotificationService
import org.mozilla.rocket.privately.browse.BrowserFragment
import org.mozilla.rocket.privately.home.PrivateHomeFragment
import org.mozilla.rocket.tabs.SessionManager
import org.mozilla.rocket.tabs.TabViewProvider
import org.mozilla.rocket.tabs.TabsSessionProvider

class PrivateModeActivity : BaseActivity(),
        FragmentListener,
        ScreenNavigator.Provider,
        ScreenNavigator.HostActivity,
        TabsSessionProvider.SessionHost {

    private var sessionManager: SessionManager? = null
    private lateinit var tabViewProvider: PrivateTabViewProvider
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var screenNavigator: ScreenNavigator

    override fun onCreate(savedInstanceState: Bundle?) {
        // we don't keep any state if user leave Private-mode
        super.onCreate(null)

        tabViewProvider = PrivateTabViewProvider(this)
        screenNavigator = ScreenNavigator(this)

        val exitEarly = handleIntent(intent)
        if (exitEarly) {
            pushToBack()
            return
        }

        setContentView(R.layout.activity_private_mode)

        makeStatusBarTransparent()

        initViewModel()

        screenNavigator.popToHomeScreen(false)
    }

    private fun initViewModel() {
        sharedViewModel = ViewModelProviders.of(this).get(SharedViewModel::class.java)
        sharedViewModel.urlInputState().value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPrivateMode()
        sessionManager?.destroy()
    }

    override fun applyLocale() {}

    override fun onNotified(from: Fragment, type: FragmentListener.TYPE, payload: Any?) {
        when (type) {
            TYPE.TOGGLE_PRIVATE_MODE -> pushToBack()
            TYPE.SHOW_URL_INPUT -> showUrlInput(payload)
            TYPE.DISMISS_URL_INPUT -> dismissUrlInput()
            TYPE.OPEN_URL_IN_CURRENT_TAB -> openUrl(payload)
            TYPE.OPEN_URL_IN_NEW_TAB -> openUrl(payload)
            TYPE.DROP_BROWSING_PAGES -> dropBrowserFragment()
            TYPE.SHOW_TAB_TRAY -> TabTray.show(supportFragmentManager)
            else -> {
            }
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.isStateSaved) {
            return
        }

        val handled = screenNavigator.visibleBrowserScreen?.onBackPressed() ?: false
        if (handled) {
            return
        }

        if (!this.screenNavigator.canGoBack()) {
            finish()
            return
        }

        super.onBackPressed()
    }

    override fun getSessionManager(): SessionManager {
        if (sessionManager == null) {
            sessionManager = SessionManager(tabViewProvider)
        }

        // we just created it, it definitely not null
        return sessionManager!!
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        val exitEarly = handleIntent(intent)
        if (exitEarly) {
            return
        }
    }

    private fun dropBrowserFragment() {
        stopPrivateMode()
        Toast.makeText(this, R.string.private_browsing_erase_done, Toast.LENGTH_LONG).show()
    }

    override fun getScreenNavigator(): ScreenNavigator = screenNavigator

    override fun getBrowserScreen(): BrowserScreen {
        return supportFragmentManager.findFragmentById(R.id.browser) as BrowserFragment
    }

    override fun createFirstRunScreen(): Screen {
        if (BuildConfig.DEBUG) {
            throw RuntimeException("PrivateModeActivity should never show first-run")
        }
        TODO("PrivateModeActivity should never show first-run")
    }

    override fun createHomeScreen(): HomeScreen {
        return PrivateHomeFragment.create()
    }

    override fun createUrlInputScreen(url: String?, parentFragmentTag: String?): UrlInputScreen {
        return UrlInputFragment.create(url, null, false)
    }

    private fun pushToBack() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME)
        startActivity(intent)
        overridePendingTransition(0, R.anim.pb_exit)
    }

    private fun showUrlInput(payload: Any?) {
        val url = payload?.toString() ?: ""
        screenNavigator.addUrlScreen(url)
        sharedViewModel.urlInputState().value = true
    }

    private fun dismissUrlInput() {
        screenNavigator.popUrlScreen()
        sharedViewModel.urlInputState().value = false
    }

    private fun openUrl(payload: Any?) {
        val url = payload?.toString() ?: ""

        ViewModelProviders.of(this)
                .get(SharedViewModel::class.java)
                .setUrl(url)

        dismissUrlInput()
        startPrivateMode()
        ScreenNavigator.get(this).showBrowserScreen(url, false, false)
    }

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
        TabViewProvider.purify(this)
    }

    @CheckResult
    private fun handleIntent(intent: Intent?): Boolean {

        if (intent?.action == PrivateMode.INTENT_EXTRA_SANITIZE) {
            TelemetryWrapper.erasePrivateModeNotification()
            stopPrivateMode()
            Toast.makeText(this, R.string.private_browsing_erase_done, Toast.LENGTH_LONG).show()
            finishAndRemoveTask()
            return true
        }
        return false
    }
}
