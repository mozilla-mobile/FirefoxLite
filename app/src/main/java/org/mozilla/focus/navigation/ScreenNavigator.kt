/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.focus.navigation

import android.content.Context
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import org.mozilla.focus.BuildConfig
import org.mozilla.focus.widget.BackKeyHandleable
import java.util.Arrays

/**
 * Provide a simple and clear interface to navigate between home/browser/url fragments.
 * This class only manages the relation between fragments, and the detail of transaction was
 * handled by TransactionHelper
 */
open class ScreenNavigator(activity: HostActivity?) : DefaultLifecycleObserver {
    private val transactionHelper: TransactionHelper
    private val activity: HostActivity?
    private val lifecycleCallbacks: FragmentManager.FragmentLifecycleCallbacks = object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentStarted(fm: FragmentManager, f: Fragment) {
            super.onFragmentStarted(fm, f)
            if (f is UrlInputScreen) {
                transactionHelper.onUrlInputScreenVisible(true)
            }
        }

        override fun onFragmentStopped(fm: FragmentManager, f: Fragment) {
            super.onFragmentStopped(fm, f)
            if (f is UrlInputScreen) {
                transactionHelper.onUrlInputScreenVisible(false)
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        activity!!.supportFragmentManager
            .registerFragmentLifecycleCallbacks(lifecycleCallbacks, false)
    }

    override fun onStop(owner: LifecycleOwner) {
        activity!!.supportFragmentManager
            .unregisterFragmentLifecycleCallbacks(lifecycleCallbacks)
    }

    /**
     * Simply clear every thing above browser fragment. This can be used when:
     * 1. you just want to move the old existing browser fragment to the foreground
     * 2. you've called Session#loadUrl() by yourself, and want to move that tab to the foreground.
     */
    open fun raiseBrowserScreen(animate: Boolean) {
        logMethod()
        transactionHelper.popAllScreens()
    }

    /**
     * Load target url on current/new tab and clear every thing above browser fragment
     *
     * @param url            target url
     * @param withNewTab     whether to open and load target url in a new tab
     * @param isFromExternal if this url is started from external VIEW intent, if true, the app will finish when the user click back key
     */
    open fun showBrowserScreen(url: String, withNewTab: Boolean, isFromExternal: Boolean) {
        logMethod(url, withNewTab)
        browserScreen.loadUrl(url, withNewTab, isFromExternal, Runnable { raiseBrowserScreen(true) })
    }

    fun restoreBrowserScreen(tabId: String) {
        logMethod()
        browserScreen.switchToTab(tabId)
    }

    /**
     * @return Whether user can directly see browser fragment
     */
    // TODO: Make geo dialog a view in browser fragment, so we can remove this method
    val isBrowserInForeground: Boolean
        get() {
            val result = activity!!.supportFragmentManager.backStackEntryCount == 0
            log("isBrowserInForeground: $result")
            return result
        }

    /**
     * Add a home fragment to back stack, so user can press back key to go back to previous
     * fragment. Use this when you want to start a new tab.
     */
    open fun addHomeScreen(animate: Boolean) {
        logMethod()
        val found = transactionHelper.popScreensUntil(HOME_FRAGMENT_TAG,
            TransactionHelper.EntryData.TYPE_ATTACHED,
            false)
        log("found exist home: $found")
        if (!found) {
            transactionHelper.showHomeScreen(animate,
                TransactionHelper.EntryData.TYPE_ATTACHED,
                false)
        }
        transactionHelper.executePendingTransaction()
    }

    /**
     * Clear every thing and show a home fragment. Use this when there's no tab available for showing.
     */
    open fun popToHomeScreen(animate: Boolean) {
        logMethod()
        val found = transactionHelper.popScreensUntil(HOME_FRAGMENT_TAG,
            TransactionHelper.EntryData.TYPE_ROOT, false)
        log("found exist home: $found")
        if (!found) {
            transactionHelper.showHomeScreen(animate, TransactionHelper.EntryData.TYPE_ROOT, false)
        }
        transactionHelper.executePendingTransaction()
    }

    fun addFirstRunScreen() {
        logMethod()
        transactionHelper.showFirstRun()
    }

    fun addUrlScreen(url: String?) {
        logMethod()
        val top = topFragment
        var tag = BROWSER_FRAGMENT_TAG
        if (top is HomeScreen) {
            tag = HOME_FRAGMENT_TAG
        } else if (top is BrowserScreen) {
            tag = BROWSER_FRAGMENT_TAG
        } else if (BuildConfig.DEBUG) {
            throw RuntimeException("unexpected caller of UrlInputScreen")
        }
        transactionHelper.showUrlInput(url, tag)
    }

    fun popUrlScreen() {
        logMethod()
        val top = topFragment
        if (top is UrlInputScreen) {
            transactionHelper.dismissUrlInput()
        }
    }

    val topFragment: Fragment?
        get() {
            val latest = transactionHelper.latestCommitFragment
            return latest ?: browserScreen.fragment
        }

    val visibleBrowserScreen: BrowserScreen?
        get() = if (isBrowserInForeground) browserScreen else null

    private val browserScreen: BrowserScreen
        private get() = activity!!.getBrowserScreen()

    fun canGoBack(): Boolean {
        val result = !transactionHelper.shouldFinish()
        log("canGoBack: $result")
        return result
    }

    val navigationState: LiveData<NavigationState>
        get() = Transformations.map(transactionHelper.topFragmentState
        ) { fragmentTag: String -> NavigationState(if (fragmentTag.isEmpty()) BROWSER_FRAGMENT_TAG else fragmentTag) }

    private fun logMethod(vararg args: Any) {
        if (LOG_NAVIGATION) {
            val stack = Thread.currentThread().stackTrace
            if (stack.size >= 4) {
                Log.d(LOG_TAG, stack[3].methodName + Arrays.toString(args))
            }
        }
    }

    private fun log(msg: String) {
        if (LOG_NAVIGATION) {
            Log.d(LOG_TAG, msg)
        }
    }

    private class NothingNavigated internal constructor() : ScreenNavigator(null) {
        override fun raiseBrowserScreen(animate: Boolean) {}
        override fun showBrowserScreen(url: String, withNewTab: Boolean, isFromExternal: Boolean) {}
        override fun addHomeScreen(animate: Boolean) {}
        override fun popToHomeScreen(animate: Boolean) {}
    }

    interface Provider {
        val screenNavigator: ScreenNavigator
    }

    /**
     * Contract class for ScreenNavigator
     */
    interface HostActivity : LifecycleOwner {
        val supportFragmentManager: FragmentManager
        fun createFirstRunScreen(): Screen?
        fun getBrowserScreen(): BrowserScreen
        fun createHomeScreen(): HomeScreen?
        fun createUrlInputScreen(url: String?, parentFragmentTag: String?): UrlInputScreen?
    }

    /**
     * Contract class for ScreenNavigator
     */
    // TODO: make this interface protected and fix FirstRunFragment
    interface Screen {
        val fragment: Fragment?
    }

    /**
     * Contract class for ScreenNavigator, to present a BrowserFragment
     */
    interface BrowserScreen : Screen, BackKeyHandleable {
        fun loadUrl(url: String,
                    openNewTab: Boolean,
                    isFromExternal: Boolean,
                    onViewReadyCallback: Runnable?)

        fun switchToTab(tabId: String?)
        fun goForeground()
        fun goBackground()
    }

    /**
     * Contract class for ScreenNavigator, to present a HomeFragment
     */
    interface HomeScreen : Screen {
        /* callback if the coverage by UrlInputScreen became visible */
        fun onUrlInputScreenVisible(visible: Boolean)
    }

    /**
     * Contract class for ScreenNavigator, to present an UrlInputFragment
     */
    interface UrlInputScreen : Screen
    class NavigationState internal constructor(val tag: String) {

        val isHome: Boolean
            get() = tag == HOME_FRAGMENT_TAG

        val isBrowser: Boolean
            get() = tag == BROWSER_FRAGMENT_TAG

        override fun hashCode(): Int {
            return tag.hashCode()
        }

        override fun equals(obj: Any?): Boolean {
            return if (obj is NavigationState) {
                tag == obj.tag
            } else false
        }

    }

    companion object {
        const val FIRST_RUN_FRAGMENT_TAG = "first_run"
        const val HOME_FRAGMENT_TAG = "home_screen"
        const val BROWSER_FRAGMENT_TAG = "browser_screen"
        const val URL_INPUT_FRAGMENT_TAG = "url_input_sceen"
        private const val LOG_TAG = "ScreenNavigator"
        private const val LOG_NAVIGATION = false
        @JvmStatic
        operator fun get(context: Context?): ScreenNavigator {
            if (context is Provider) {
                return (context as Provider).screenNavigator
            }
            return if (BuildConfig.DEBUG) {
                throw RuntimeException("the given context should implement ScreenNavigator.Provider")
            } else {
                NothingNavigated()
            }
        }
    }

    init {
        if (activity == null) {
            return
        }
        this.activity = activity
        transactionHelper = TransactionHelper(activity!!)
        this.activity!!.lifecycle.addObserver(transactionHelper)
        this.activity.lifecycle.addObserver(this)
    }
}