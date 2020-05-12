/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.focus.navigation

import android.text.TextUtils
import android.view.animation.Animation
import androidx.annotation.IntDef
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.mozilla.focus.R
import org.mozilla.focus.navigation.ScreenNavigator.BrowserScreen
import org.mozilla.focus.navigation.ScreenNavigator.HomeScreen
import org.mozilla.focus.navigation.ScreenNavigator.HostActivity
import org.mozilla.focus.navigation.TransactionHelper.EntryData.EntryType

internal class TransactionHelper(private val activity: HostActivity) : DefaultLifecycleObserver {
    private var backStackListener: BackStackListener? = null
    private val topFragmentState = MutableLiveData<TopFragmentState>()

    init {
        registerBackStackListener()
    }

    override fun onStart(owner: LifecycleOwner) {
        registerBackStackListener()
    }

    override fun onStop(owner: LifecycleOwner) {
        unregisterBackStackListener()
    }

    fun showHomeScreen(animated: Boolean, @EntryType type: Int, executeImmediatly: Boolean) {
        if (isStateSaved) {
            return
        }
        prepareHomeScreen(animated, type).commit()
        if (executeImmediatly) {
            activity.getSupportFragmentManager().executePendingTransactions()
        }
    }

    fun showFirstRun() {
        if (isStateSaved) {
            return
        }
        prepareFirstRun().commit()
    }

    fun showUrlInput(url: String?, sourceFragment: String) {
        if (isStateSaved) {
            return
        }
        val fragmentManager = activity.getSupportFragmentManager()
        val existingFragment = fragmentManager.findFragmentByTag(ScreenNavigator.URL_INPUT_FRAGMENT_TAG)
        if (existingFragment != null && existingFragment.isAdded && !existingFragment.isRemoving) {
            // We are already showing an URL input fragment. This might have been a double click on the
            // fake URL bar. Just ignore it.
            return
        }
        prepareUrlInput(url, sourceFragment)
            .addToBackStack(makeEntryTag(ScreenNavigator.URL_INPUT_FRAGMENT_TAG, EntryData.TYPE_FLOATING))
            .commit()
    }

    fun dismissUrlInput() {
        val mgr = activity.getSupportFragmentManager()
        if (mgr.isStateSaved) {
            return
        }
        mgr.popBackStack()
    }

    fun shouldFinish(): Boolean {
        val manager = activity.getSupportFragmentManager()
        val entryCount = manager.backStackEntryCount
        if (entryCount == 0) {
            return true
        }
        val lastEntry = manager.getBackStackEntryAt(entryCount - 1)
        return EntryData.TYPE_ROOT == getEntryType(lastEntry)
    }

    fun popAllScreens() {
        val manager = activity.getSupportFragmentManager()
        var entryCount = manager.backStackEntryCount
        while (entryCount > 0) {
            manager.popBackStack()
            entryCount--
        }
        manager.executePendingTransactions()
    }

    fun popScreensUntil(targetEntryName: String?, @EntryType type: Int, executeImmediately: Boolean): Boolean {
        val clearAll = targetEntryName == null
        val manager = activity.getSupportFragmentManager()
        var entryCount = manager.backStackEntryCount
        var found = false
        while (entryCount > 0) {
            val entry = manager.getBackStackEntryAt(entryCount - 1)
            if (!clearAll && TextUtils.equals(targetEntryName, getEntryTag(entry)) && type == getEntryType(entry)) {
                found = true
                break
            }
            manager.popBackStack()
            entryCount--
        }
        if (executeImmediately) {
            manager.executePendingTransactions()
        }
        return found
    }

    val latestCommitFragment: Fragment?
        get() {
            val manager = activity.getSupportFragmentManager()
            val count = manager.backStackEntryCount
            if (count == 0) {
                return null
            }
            val tag = getFragmentTag(count - 1)
            return manager.findFragmentByTag(tag)
        }

    fun getTopFragmentState(): LiveData<TopFragmentState> {
        return topFragmentState
    }

    fun executePendingTransaction() {
        activity.getSupportFragmentManager().executePendingTransactions()
    }

    private fun prepareFirstRun(): FragmentTransaction {
        val fragmentManager = activity.getSupportFragmentManager()
        val screen = activity.createFirstRunScreen()
        val transaction = fragmentManager.beginTransaction()
        if (fragmentManager.findFragmentByTag(ScreenNavigator.FIRST_RUN_FRAGMENT_TAG) == null) {
            transaction.replace(R.id.container, screen.getFragment(), ScreenNavigator.FIRST_RUN_FRAGMENT_TAG)
                .addToBackStack(makeEntryTag(ScreenNavigator.FIRST_RUN_FRAGMENT_TAG, EntryData.TYPE_ROOT))
        }
        return transaction
    }

    private fun prepareHomeScreen(animated: Boolean, @EntryType type: Int): FragmentTransaction {
        val fragmentManager = activity.getSupportFragmentManager()
        val homeScreen = activity.createHomeScreen()
        val transaction = fragmentManager.beginTransaction()
        val enterAnim = if (animated) R.anim.tab_transition_fade_in else 0
        val exitAnim = if (type == EntryData.TYPE_ROOT) 0 else R.anim.tab_transition_fade_out
        transaction.setCustomAnimations(enterAnim, 0, 0, exitAnim)
        transaction.add(R.id.container, homeScreen.getFragment(), ScreenNavigator.HOME_FRAGMENT_TAG)
        transaction.addToBackStack(makeEntryTag(ScreenNavigator.HOME_FRAGMENT_TAG, type))
        return transaction
    }

    private fun prepareUrlInput(url: String?, parentFragmentTag: String): FragmentTransaction {
        val fragmentManager = activity.getSupportFragmentManager()
        val urlScreen: ScreenNavigator.Screen = activity.createUrlInputScreen(url, parentFragmentTag)
        val transaction = fragmentManager.beginTransaction()
        transaction.add(R.id.container, urlScreen.getFragment(), ScreenNavigator.URL_INPUT_FRAGMENT_TAG)
        return transaction
    }

    fun onUrlInputScreenVisible(visible: Boolean) {
        val fragmentManager = activity.getSupportFragmentManager()
        val homeFragment = fragmentManager.findFragmentByTag(ScreenNavigator.HOME_FRAGMENT_TAG) as ScreenNavigator.Screen?
        if (homeFragment != null && homeFragment.getFragment().isVisible) {
            if (homeFragment is HomeScreen) {
                homeFragment.onUrlInputScreenVisible(visible)
            }
        }
    }

    private fun getFragmentTag(backStackIndex: Int): String {
        val manager = activity.getSupportFragmentManager()
        return getEntryTag(manager.getBackStackEntryAt(backStackIndex))
    }

    private fun getEntryTag(entry: FragmentManager.BackStackEntry): String {
        return entry.name?.let {
            it.split(ENTRY_TAG_SEPARATOR).toTypedArray()[0]
        } ?: ""
    }

    @EntryType
    private fun getEntryType(entry: FragmentManager.BackStackEntry): Int {
        return entry.name?.let {
            it.split(ENTRY_TAG_SEPARATOR).toTypedArray()[1].toInt()
        } ?: EntryData.TYPE_ROOT
    }

    private fun makeEntryTag(tag: String, @EntryType type: Int): String {
        return tag + ENTRY_TAG_SEPARATOR + type
    }

    private val isStateSaved: Boolean
        get() = activity.getSupportFragmentManager().isStateSaved

    private fun onFragmentBroughtToFront(fragmentTag: String, parentFragmentTag: String) {
        topFragmentState.value = TopFragmentState(fragmentTag, parentFragmentTag)
    }

    private fun registerBackStackListener() {
        if (backStackListener == null) {
            backStackListener = BackStackListener(this)
            backStackListener?.let {
                activity.getSupportFragmentManager().addOnBackStackChangedListener(it)
            }
        }
    }

    private fun unregisterBackStackListener() {
        backStackListener?.let {
            activity.getSupportFragmentManager().removeOnBackStackChangedListener(it)
            it.onStop()
        }
        backStackListener = null
    }

    private class BackStackListener internal constructor(helper: TransactionHelper) : FragmentManager.OnBackStackChangedListener {
        private var stateRunnable: Runnable? = null
        private var helper: TransactionHelper?

        init {
            this.helper = helper
            // set up initial states
            notifyTopFragment(helper.activity.getSupportFragmentManager())
        }

        override fun onBackStackChanged() {
            helper?.let {
                val manager = it.activity.getSupportFragmentManager()
                val fragment = manager.findFragmentById(R.id.browser)
                notifyTopFragment(manager)
                if (fragment is BrowserScreen) {
                    setBrowserState(shouldKeepBrowserRunning(it), it)
                }
            }
        }

        fun onStop() {
            helper = null
            stateRunnable = null
        }

        private fun shouldKeepBrowserRunning(helper: TransactionHelper): Boolean {
            val manager = helper.activity.getSupportFragmentManager()
            val entryCount = manager.backStackEntryCount
            for (i in entryCount - 1 downTo 0) {
                val entry = manager.getBackStackEntryAt(i)
                if (helper.getEntryType(entry) != EntryData.TYPE_FLOATING) {
                    return false
                }
            }
            return true
        }

        private fun setBrowserState(isForeground: Boolean, helper: TransactionHelper) {
            stateRunnable = Runnable { setBrowserForegroundState(isForeground) }
            val actor = getTopAnimationAccessibleFragment(helper)
            var anim: Animation? = null
            if (actor == null || actor.customEnterTransition.also { anim = it } == null || anim?.hasEnded() == true) {
                executeStateRunnable()
            } else {
                anim?.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation) {}
                    override fun onAnimationEnd(animation: Animation) {
                        executeStateRunnable()
                    }

                    override fun onAnimationRepeat(animation: Animation) {}
                })
            }
        }

        private fun setBrowserForegroundState(isForeground: Boolean) {
            helper?.let {
                val manager = it.activity.getSupportFragmentManager()
                val browserFragment = manager.findFragmentById(R.id.browser) as BrowserScreen
                if (isForeground) {
                    browserFragment.goForeground()
                } else {
                    browserFragment.goBackground()
                }
            }
        }

        private fun executeStateRunnable() {
            stateRunnable?.run()
            stateRunnable = null
        }

        private fun notifyTopFragment(manager: FragmentManager) {
            val entryCount = manager.backStackEntryCount
            var fragmentTag = ""
            if (entryCount > 0) {
                val entry = manager.getBackStackEntryAt(entryCount - 1)
                helper?.let {
                    fragmentTag = it.getEntryTag(entry)
                }
            }
            var parentFragmentTag = ""
            if (entryCount > 1) {
                val entry = manager.getBackStackEntryAt(entryCount - 2)
                helper?.let {
                    parentFragmentTag = it.getEntryTag(entry)
                }
            }
            helper?.onFragmentBroughtToFront(fragmentTag, parentFragmentTag)
        }

        fun getTopAnimationAccessibleFragment(helper: TransactionHelper): FragmentAnimationAccessor? {
            val top = helper.latestCommitFragment
            return if (top != null && top is FragmentAnimationAccessor) {
                top
            } else null
        }
    }

    internal object EntryData {
        /**
         * argument passed to [FragmentTransaction.addToBackStack], pressing back when this
         * type of fragment is in foreground will close the app
         */
        const val TYPE_ROOT = 0

        /**
         * argument passed to [FragmentTransaction.addToBackStack], adding fragment of
         * this type will make browser fragment go to background
         */
        const val TYPE_ATTACHED = 1

        /**
         * argument passed to [FragmentTransaction.addToBackStack], browsing fragment
         * will still be in foreground after adding this type of fragment.
         */
        const val TYPE_FLOATING = 2

        @IntDef(TYPE_ROOT, TYPE_ATTACHED, TYPE_FLOATING)
        internal annotation class EntryType
    }

    data class TopFragmentState(val topFragmentTag: String, val parentFragmentTag: String)

    companion object {
        private const val ENTRY_TAG_SEPARATOR = "#"
    }
}
