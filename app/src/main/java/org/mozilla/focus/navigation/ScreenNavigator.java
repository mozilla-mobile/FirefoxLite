/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.navigation;

import android.arch.lifecycle.DefaultLifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;

import org.mozilla.focus.BuildConfig;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.widget.BackKeyHandleable;

import java.util.Arrays;

/**
 * Provide a simple and clear interface to navigate between home/browser/url fragments.
 * This class only manages the relation between fragments, and the detail of transaction was
 * handled by TransactionHelper
 */
public class ScreenNavigator implements DefaultLifecycleObserver {
    public static final String FIRST_RUN_FRAGMENT_TAG = "first_run";
    public static final String HOME_FRAGMENT_TAG = "home_screen";
    public static final String BROWSER_FRAGMENT_TAG = "browser_screen";
    public static final String URL_INPUT_FRAGMENT_TAG = "url_input_sceen";

    private static final String LOG_TAG = "ScreenNavigator";
    private static final boolean LOG_NAVIGATION = false;

    private TransactionHelper transactionHelper;

    private HostActivity activity;

    private FragmentManager.FragmentLifecycleCallbacks lifecycleCallbacks = new FragmentManager.FragmentLifecycleCallbacks() {
        @Override
        public void onFragmentStarted(FragmentManager fm, Fragment f) {
            super.onFragmentStarted(fm, f);
            if (f instanceof UrlInputScreen) {
                ScreenNavigator.this.transactionHelper.onUrlInputScreenVisible(true);
            }
        }

        @Override
        public void onFragmentStopped(FragmentManager fm, Fragment f) {
            super.onFragmentStopped(fm, f);
            if (f instanceof UrlInputScreen) {
                ScreenNavigator.this.transactionHelper.onUrlInputScreenVisible(false);
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

    public ScreenNavigator(@Nullable HostActivity activity) {
        if (activity == null) {
            return;
        }
        this.activity = activity;
        this.transactionHelper = new TransactionHelper(activity);

        this.activity.getLifecycle().addObserver(this.transactionHelper);
        this.activity.getLifecycle().addObserver(this);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        this.activity.getSupportFragmentManager()
                .registerFragmentLifecycleCallbacks(lifecycleCallbacks, false);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        this.activity.getSupportFragmentManager()
                .unregisterFragmentLifecycleCallbacks(lifecycleCallbacks);
    }

    /**
     * Simply clear every thing above browser fragment. This can be used when:
     * 1. you just want to move the old existing browser fragment to the foreground
     * 2. you've called Session#loadUrl() by yourself, and want to move that tab to the foreground.
     */
    public void raiseBrowserScreen(boolean animate) {
        TelemetryWrapper.statsRaiseBrowserScreen();
        logMethod();

        this.transactionHelper.popAllScreens();
    }

    /**
     * Load target url on current/new tab and clear every thing above browser fragment
     *
     * @param url            target url
     * @param withNewTab     whether to open and load target url in a new tab
     * @param isFromExternal if this url is started from external VIEW intent, if true, the app will finish when the user click back key
     */
    public void showBrowserScreen(String url, boolean withNewTab, boolean isFromExternal) {
        logMethod(url, withNewTab);

        getBrowserScreen().loadUrl(url, withNewTab, isFromExternal, () -> raiseBrowserScreen(true));
    }

    public void restoreBrowserScreen(@NonNull String tabId) {
        logMethod();
        getBrowserScreen().switchToTab(tabId);
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

        boolean found = this.transactionHelper.popScreensUntil(HOME_FRAGMENT_TAG, TransactionHelper.EntryData.TYPE_ATTACHED);
        log("found exist home: " + found);
        if (!found) {
            this.transactionHelper.showHomeScreen(animate, TransactionHelper.EntryData.TYPE_ATTACHED);
        }
    }

    /**
     * Clear every thing and show a home fragment. Use this when there's no tab available for showing.
     */
    public void popToHomeScreen(boolean animate) {
        logMethod();

        boolean found = this.transactionHelper.popScreensUntil(HOME_FRAGMENT_TAG,
                TransactionHelper.EntryData.TYPE_ROOT);
        log("found exist home: " + found);
        if (!found) {
            this.transactionHelper.showHomeScreen(animate, TransactionHelper.EntryData.TYPE_ROOT);
        }
    }

    public void addFirstRunScreen() {
        logMethod();
        this.transactionHelper.showFirstRun();
    }

    public void addUrlScreen(String url) {
        logMethod();
        Fragment top = getTopFragment();

        String tag = BROWSER_FRAGMENT_TAG;
        if (top instanceof HomeScreen) {
            tag = HOME_FRAGMENT_TAG;
        } else if (top instanceof BrowserScreen) {
            tag = BROWSER_FRAGMENT_TAG;
        } else if (BuildConfig.DEBUG) {
            throw new RuntimeException("unexpected caller of UrlInputScreen");
        }

        this.transactionHelper.showUrlInput(url, tag);
    }

    public void popUrlScreen() {
        logMethod();
        Fragment top = getTopFragment();
        if (top instanceof UrlInputScreen) {
            this.transactionHelper.dismissUrlInput();
        }
    }

    @Nullable
    public Fragment getTopFragment() {
        Fragment latest = this.transactionHelper.getLatestCommitFragment();
        return (latest == null) ? getBrowserScreen().getFragment() : latest;
    }

    public BrowserScreen getVisibleBrowserScreen() {
        return isBrowserInForeground() ? getBrowserScreen() : null;
    }

    private BrowserScreen getBrowserScreen() {
        return activity.getBrowserScreen();
    }

    public boolean canGoBack() {
        boolean result = !transactionHelper.shouldFinish();
        log("canGoBack: " + result);
        return result;
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

    /**
     * Contract class for ScreenNavigator
     */
    public interface HostActivity extends LifecycleOwner {
        FragmentManager getSupportFragmentManager();

        Screen createFirstRunScreen();

        BrowserScreen getBrowserScreen();

        HomeScreen createHomeScreen();

        UrlInputScreen createUrlInputScreen(@Nullable String url, String parentFragmentTag);

    }

    /**
     * Contract class for ScreenNavigator
     */
    public interface Screen {
        Fragment getFragment();
    }

    /**
     * Contract class for ScreenNavigator, to present a BrowserFragment
     */
    public interface BrowserScreen extends Screen, BackKeyHandleable {
        void loadUrl(@NonNull final String url,
                     boolean openNewTab,
                     boolean isFromExternal,
                     final Runnable onViewReadyCallback);

        void switchToTab(final String tabId);

        void goForeground();

        void goBackground();
    }

    /**
     * Contract class for ScreenNavigator, to present a HomeFragment
     */
    public interface HomeScreen extends Screen {
        /* callback if the coverage by UrlInputScreen became visible */
        void onUrlInputScreenVisible(boolean visible);
    }

    /**
     * Contract class for ScreenNavigator, to present an UrlInputFragment
     */
    public interface UrlInputScreen extends Screen {
    }
}
