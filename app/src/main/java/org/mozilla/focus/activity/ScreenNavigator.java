/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.content.Context;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.util.Log;

import org.mozilla.focus.BuildConfig;

import java.util.Arrays;

/**
 * This class tends to provide a simple and clear interface to fragments that need to
 * navigate to home/browser screen.
 *
 * TODO: Gradually improve this class
 * 1. Move more methods from MainMediator to ScreenNavigator
 * 2. Finally remove MainMediator from MainActivity
 */
public class ScreenNavigator {
    @SuppressWarnings("WeakerAccess")
//    public static final int FROM_HOME = 0;
//    public static final int FROM_BROWSER = 1;
//    public static final int FROM_TAB_TRAY = 2;
//    @IntDef({FROM_HOME, FROM_BROWSER, FROM_TAB_TRAY})
//    private @interface SourcePage {}

    private static final String LOG_TAG = "ScreenNavigator";
    private static final boolean LOG_NAVIGATION = true;

    private MainMediator mainMediator;
    private BrowserMediator browserMediator;

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

    ScreenNavigator(MainActivity activity) {
        this.mainMediator = new MainMediator(activity);
        this.browserMediator = new BrowserMediator(activity, mainMediator);
    }

    /**
     * Simply clear every thing above browser fragment. This can be used when:
     * 1. you just want to move the old existing browser fragment to the foreground
     * 2. you've called Tab#loadUrl() by yourself, and want to move that tab to the foreground.
     */
    public void raiseBrowserScreen() {
        logMethod();

        this.browserMediator.raiseBrowserScreen(true);
    }

    /**
     * Load target url on current/new tab and clear every thing above browser fragment
     * @param url target url
     * @param withNewTab whether to open and load target url in a new tab
     */
    public void showBrowserScreen(String url, boolean withNewTab) {
        logMethod(url, withNewTab);

        this.browserMediator.showBrowserScreen(url, withNewTab);
    }

    void restoreBrowserScreen(@NonNull String tabId) {
        this.browserMediator.showBrowserScreenForRestoreTabs(tabId);
    }

    /**
     * @return Whether user can directly see browser fragment
     */
    public boolean isBrowserInForeground() {
        return this.mainMediator.getTopHomeFragment() == null;
    }

    /**
     * Add a home fragment to back stack, so user can press back key to go back to previous
     * fragment. Use this when you want to start a new tab.
     */
    public void addHomeScreen(boolean animate) {
        logMethod();

        this.mainMediator.showHomeScreen(animate, true);
    }

    /**
     * Clear every thing and show a home fragment. Use this when there's no tab available for showing.
     */
    public void popToHomeScreen(boolean animate) {
        logMethod();

        this.mainMediator.clearAllFragment(animate);
        this.mainMediator.showHomeScreen(animate, false);
    }

    private void logMethod(Object... args) {
        if (LOG_NAVIGATION) {
            StackTraceElement stack[] = Thread.currentThread().getStackTrace();
            if (stack.length >= 4) {
                Log.d(LOG_TAG, stack[3].getMethodName() + Arrays.toString(args));
            }
        }
    }

    private static class NothingNavigated extends ScreenNavigator {
        NothingNavigated() {
            super(null);
        }

        @Override
        public void raiseBrowserScreen() {
        }

        @Override
        public void showBrowserScreen(String url, boolean withNewTab) {
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
