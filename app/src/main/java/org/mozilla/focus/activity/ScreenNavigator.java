/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.util.Log;

import org.mozilla.focus.BuildConfig;
import org.mozilla.focus.R;
import org.mozilla.focus.fragment.BrowserFragment;
import org.mozilla.focus.home.HomeFragment;
import org.mozilla.focus.urlinput.UrlInputFragment;

import java.util.Arrays;

/**
 * This class tends to provide a simple and clear interface to fragments that need to
 * navigate to home/browser screen.
 *
 * TODO: Gradually improve this class
 * 1. Move more methods from MainMediator to ScreenNavigator
 * 2. Finally remove MainMediator from MainActivity
 */
public class ScreenNavigator implements LifecycleObserver {
    private static final String LOG_TAG = "ScreenNavigator";
    private static final boolean LOG_NAVIGATION = false;

    private MainMediator mainMediator;

    private MainActivity activity;

    private FragmentManager.FragmentLifecycleCallbacks lifecycleCallbacks = new FragmentManager.FragmentLifecycleCallbacks() {
        @Override
        public void onFragmentStarted(FragmentManager fm, Fragment f) {
            super.onFragmentStarted(fm, f);
            if (f instanceof UrlInputFragment) {
                ScreenNavigator.this.mainMediator.toggleFakeUrlInput(false);
            }
        }

        @Override
        public void onFragmentStopped(FragmentManager fm, Fragment f) {
            super.onFragmentStopped(fm, f);
            if (f instanceof UrlInputFragment) {
                ScreenNavigator.this.mainMediator.toggleFakeUrlInput(true);
            }
        }
    };

    public static ScreenNavigator get(Context context) {
        if (context instanceof Provider) {
            return ((Provider) context).getScreenNavigator();
        }

        if (BuildConfig.DEBUG) {
            throw new RuntimeException("the given context should implement ScreenNavigator.Provider");
        } else {
            return new NothingNavigated();
        }
    }

    ScreenNavigator(@Nullable MainActivity activity) {
        if (activity == null) {
            return;
        }
        this.mainMediator = new MainMediator(activity);
        this.activity = activity;
        this.activity.getLifecycle().addObserver(this);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void registerListeners() {
        this.activity.getSupportFragmentManager()
                .registerFragmentLifecycleCallbacks(lifecycleCallbacks, false);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void unregisterListeners() {
        this.activity.getSupportFragmentManager()
                .unregisterFragmentLifecycleCallbacks(lifecycleCallbacks);
    }

    /**
     * Simply clear every thing above browser fragment. This can be used when:
     * 1. you just want to move the old existing browser fragment to the foreground
     * 2. you've called Tab#loadUrl() by yourself, and want to move that tab to the foreground.
     */
    public void raiseBrowserScreen(boolean animate) {
        logMethod();

        popAllScreens();
        this.activity.sendBrowsingTelemetry();
    }

    /**
     * Load target url on current/new tab and clear every thing above browser fragment
     * @param url target url
     * @param withNewTab whether to open and load target url in a new tab
     * @param isFromExternal if this url is started from external VIEW intent
     */
    public void showBrowserScreen(String url, boolean withNewTab, boolean isFromExternal) {
        logMethod(url, withNewTab);

        getBrowserFragment().loadUrl(url, withNewTab, isFromExternal, () -> raiseBrowserScreen(true));
    }

    void restoreBrowserScreen(@NonNull String tabId) {
        logMethod();

        getBrowserFragment().loadTab(tabId);
        raiseBrowserScreen(false);
    }

    /**
     * @return Whether user can directly see browser fragment
     */
    // TODO: Make geo dialog a view in browser fragment, so we can remove this method
    public boolean isBrowserInForeground() {
        boolean result = (activity.getSupportFragmentManager().getBackStackEntryCount() == 0);
        log("isBrowserInForeground: " + result);
        return result;
    }

    /**
     * Add a home fragment to back stack, so user can press back key to go back to previous
     * fragment. Use this when you want to start a new tab.
     */
    public void addHomeScreen(boolean animate) {
        logMethod();

        boolean found = popScreensUntil(HomeFragment.FRAGMENT_TAG);
        log("found exist home: " + found);
        if (!found) {
            this.mainMediator.showHomeScreen(animate, MainMediator.EntryData.TYPE_FLOATING);
        }
    }

    /**
     * Clear every thing and show a home fragment. Use this when there's no tab available for showing.
     */
    public void popToHomeScreen(boolean animate) {
        logMethod();

        boolean found = popScreensUntil(HomeFragment.FRAGMENT_TAG);
        log("found exist home: " + found);
        if (found) {
            this.mainMediator.updateForegroundType(MainMediator.EntryData.TYPE_ROOT);
        } else {
            this.mainMediator.showHomeScreen(animate, MainMediator.EntryData.TYPE_ROOT);
        }
    }

    public void addFirstRunScreen() {
        logMethod();
        this.mainMediator.showFirstRun();
    }

    public void addUrlScreen(String url) {
        logMethod();
        Fragment top = getTopFragment();

        String tag = BrowserFragment.FRAGMENT_TAG;
        if (top instanceof HomeFragment) {
            tag = HomeFragment.FRAGMENT_TAG;
        } else if (top instanceof BrowserFragment) {
            tag = BrowserFragment.FRAGMENT_TAG;
        } else if (BuildConfig.DEBUG) {
            throw new RuntimeException("unexpected caller of UrlInputFragment");
        }

        this.mainMediator.showUrlInput(url, tag);
    }

    public void popUrlScreen() {
        logMethod();
        Fragment top = getTopFragment();
        if (top instanceof UrlInputFragment) {
            this.mainMediator.dismissUrlInput();
        }
    }

    @Nullable
    public Fragment getTopFragment() {
        FragmentManager manager = this.activity.getSupportFragmentManager();
        int count = manager.getBackStackEntryCount();
        if (count == 0) {
            return getBrowserFragment();
        }

        String tag = this.mainMediator.getEntryTag(manager.getBackStackEntryAt(count - 1));
        return manager.findFragmentByTag(tag);
    }


    private BrowserFragment getBrowserFragment() {
        return (BrowserFragment) activity.getSupportFragmentManager().findFragmentById(R.id.browser);
    }

    public boolean canGoBack() {
        boolean result = !mainMediator.shouldFinish();
        log("canGoBack: " + result);
        return result;
    }

    private void popAllScreens() {
        popScreensUntil(null);
    }

    private boolean popScreensUntil(@Nullable String targetEntryName) {
        boolean clearAll = (targetEntryName == null);
        FragmentManager manager = activity.getSupportFragmentManager();
        int entryCount = manager.getBackStackEntryCount();
        boolean found = false;
        while (entryCount > 0) {
            FragmentManager.BackStackEntry entry = manager.getBackStackEntryAt(entryCount - 1);
            if (!clearAll && TextUtils.equals(targetEntryName, this.mainMediator.getEntryTag(entry))) {
                found = true;
                break;
            }
            manager.popBackStack();
            entryCount--;
        }
        manager.executePendingTransactions();
        return found;
    }

    private void logMethod(Object... args) {
        if (LOG_NAVIGATION) {
            StackTraceElement stack[] = Thread.currentThread().getStackTrace();
            if (stack.length >= 4) {
                Log.d(LOG_TAG, stack[3].getMethodName() + Arrays.toString(args));
            }
        }
    }

    private void log(String msg) {
        if (LOG_NAVIGATION) {
            Log.d(LOG_TAG, msg);
        }
    }

    private static class NothingNavigated extends ScreenNavigator {
        NothingNavigated() {
            super(null);
        }

        @Override
        public void raiseBrowserScreen(boolean animate) {
        }

        @Override
        public void showBrowserScreen(String url, boolean withNewTab, boolean isFromExternal) {
        }

        @Override
        public void addHomeScreen(boolean animate) {
        }

        @Override
        public void popToHomeScreen(boolean animate) {
        }
    }

    public interface Provider {
        ScreenNavigator getScreenNavigator();
    }
}
