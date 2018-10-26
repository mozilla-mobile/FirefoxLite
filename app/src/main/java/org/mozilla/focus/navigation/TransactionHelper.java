/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.navigation;

import android.arch.lifecycle.DefaultLifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.animation.Animation;

import org.mozilla.focus.R;

import static org.mozilla.focus.navigation.ScreenNavigator.BrowserScreen;
import static org.mozilla.focus.navigation.ScreenNavigator.FIRST_RUN_FRAGMENT_TAG;
import static org.mozilla.focus.navigation.ScreenNavigator.HOME_FRAGMENT_TAG;
import static org.mozilla.focus.navigation.ScreenNavigator.HomeScreen;
import static org.mozilla.focus.navigation.ScreenNavigator.URL_INPUT_FRAGMENT_TAG;

class TransactionHelper implements DefaultLifecycleObserver {
    private final ScreenNavigator.HostActivity activity;
    private BackStackListener backStackListener;

    private static final String ENTRY_TAG_SEPARATOR = "#";

    TransactionHelper(@NonNull ScreenNavigator.HostActivity activity) {
        this.activity = activity;
        this.backStackListener = new BackStackListener(this);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        this.activity.getSupportFragmentManager()
                .addOnBackStackChangedListener(this.backStackListener = new BackStackListener(this));
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        this.activity.getSupportFragmentManager()
                .removeOnBackStackChangedListener(this.backStackListener);
        this.backStackListener.onStop();
    }

    void showHomeScreen(boolean animated, @EntryData.EntryType int type) {
        if (isStateSaved()) {
            return;
        }
        this.prepareHomeScreen(animated, type).commit();
        this.activity.getSupportFragmentManager().executePendingTransactions();
    }

    void showFirstRun() {
        if (isStateSaved()) {
            return;
        }
        this.prepareFirstRun().commit();
    }

    void showUrlInput(@Nullable String url, String sourceFragment) {
        if (isStateSaved()) {
            return;
        }
        final FragmentManager fragmentManager = this.activity.getSupportFragmentManager();
        final Fragment existingFragment = fragmentManager.findFragmentByTag(URL_INPUT_FRAGMENT_TAG);
        if (existingFragment != null && existingFragment.isAdded() && !existingFragment.isRemoving()) {
            // We are already showing an URL input fragment. This might have been a double click on the
            // fake URL bar. Just ignore it.
            return;
        }

        this.prepareUrlInput(url, sourceFragment)
                .addToBackStack(makeEntryTag(URL_INPUT_FRAGMENT_TAG, EntryData.TYPE_FLOATING))
                .commit();
    }

    void dismissUrlInput() {
        final FragmentManager mgr = this.activity.getSupportFragmentManager();
        if (mgr.isStateSaved()) {
            return;
        }
        mgr.popBackStack();
    }

    boolean shouldFinish() {
        FragmentManager manager = this.activity.getSupportFragmentManager();
        int entryCount = manager.getBackStackEntryCount();
        if (entryCount == 0) {
            return true;
        }

        FragmentManager.BackStackEntry lastEntry = manager.getBackStackEntryAt(entryCount - 1);
        return EntryData.TYPE_ROOT == getEntryType(lastEntry);
    }

    void popAllScreens() {
        FragmentManager manager = this.activity.getSupportFragmentManager();
        int entryCount = manager.getBackStackEntryCount();
        while (entryCount > 0) {
            if (!manager.isStateSaved()) {
                // Can not perform this action after onSaveInstanceState
                manager.popBackStack();
                entryCount--;
            }
        }
        manager.executePendingTransactions();
    }

    boolean popScreensUntil(@Nullable String targetEntryName, @EntryData.EntryType int type) {
        boolean clearAll = (targetEntryName == null);
        FragmentManager manager = this.activity.getSupportFragmentManager();
        int entryCount = manager.getBackStackEntryCount();
        boolean found = false;
        while (entryCount > 0) {
            FragmentManager.BackStackEntry entry = manager.getBackStackEntryAt(entryCount - 1);
            if (!clearAll
                    && TextUtils.equals(targetEntryName, getEntryTag(entry))
                    && type == getEntryType(entry)) {
                found = true;
                break;
            }
            manager.popBackStack();
            entryCount--;
        }
        manager.executePendingTransactions();
        return found;
    }

    @Nullable
    Fragment getLatestCommitFragment() {
        FragmentManager manager = this.activity.getSupportFragmentManager();
        int count = manager.getBackStackEntryCount();
        if (count == 0) {
            return null;
        }

        String tag = getFragmentTag(count - 1);
        return manager.findFragmentByTag(tag);
    }

    private FragmentTransaction prepareFirstRun() {
        final FragmentManager fragmentManager = this.activity.getSupportFragmentManager();
        final ScreenNavigator.Screen screen = this.activity.createFirstRunScreen();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (fragmentManager.findFragmentByTag(FIRST_RUN_FRAGMENT_TAG) == null) {
            transaction.replace(R.id.container, screen.getFragment(), FIRST_RUN_FRAGMENT_TAG)
                    .addToBackStack(makeEntryTag(FIRST_RUN_FRAGMENT_TAG, EntryData.TYPE_ROOT));
        }

        return transaction;
    }

    private FragmentTransaction prepareHomeScreen(boolean animated, @EntryData.EntryType int type) {
        final FragmentManager fragmentManager = this.activity.getSupportFragmentManager();
        final HomeScreen homeScreen = this.activity.createHomeScreen();

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        int enterAnim = animated ? R.anim.tab_transition_fade_in : 0;
        int exitAnim = (type == EntryData.TYPE_ROOT) ? 0 : R.anim.tab_transition_fade_out;
        transaction.setCustomAnimations(enterAnim, 0, 0, exitAnim);

        transaction.add(R.id.container, homeScreen.getFragment(), HOME_FRAGMENT_TAG);
        transaction.addToBackStack(makeEntryTag(HOME_FRAGMENT_TAG, type));

        return transaction;
    }

    private FragmentTransaction prepareUrlInput(@Nullable String url, String parentFragmentTag) {
        final FragmentManager fragmentManager = this.activity.getSupportFragmentManager();
        final ScreenNavigator.Screen urlScreen = this.activity.createUrlInputScreen(url, parentFragmentTag);
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.container, urlScreen.getFragment(), URL_INPUT_FRAGMENT_TAG);
        return transaction;
    }

    void onUrlInputScreenVisible(boolean visible) {
        final FragmentManager fragmentManager = this.activity.getSupportFragmentManager();
        final ScreenNavigator.Screen homeFragment =
                (ScreenNavigator.Screen) fragmentManager.findFragmentByTag(HOME_FRAGMENT_TAG);
        if (homeFragment != null && homeFragment.getFragment().isVisible()) {
            if (homeFragment instanceof HomeScreen) {
                ((HomeScreen) homeFragment).onUrlInputScreenVisible(visible);
            }
        }
    }

    private String getFragmentTag(int backStackIndex) {
        FragmentManager manager = this.activity.getSupportFragmentManager();
        return getEntryTag(manager.getBackStackEntryAt(backStackIndex));
    }

    private String getEntryTag(FragmentManager.BackStackEntry entry) {
        String result[] = entry.getName().split(ENTRY_TAG_SEPARATOR);
        return result[0];
    }

    private @EntryData.EntryType
    int getEntryType(FragmentManager.BackStackEntry entry) {
        String result[] = entry.getName().split(ENTRY_TAG_SEPARATOR);
        return Integer.parseInt(result[1]);
    }

    private String makeEntryTag(String tag, @EntryData.EntryType int type) {
        return tag + ENTRY_TAG_SEPARATOR + type;
    }

    private boolean isStateSaved() {
        FragmentManager manager = this.activity.getSupportFragmentManager();
        return manager == null || manager.isStateSaved();
    }

    private static class BackStackListener implements FragmentManager.OnBackStackChangedListener {
        private Runnable stateRunnable;
        private TransactionHelper helper;

        BackStackListener(TransactionHelper helper) {
            this.helper = helper;
        }

        @Override
        public void onBackStackChanged() {
            if (this.helper == null) {
                return;
            }

            FragmentManager manager = this.helper.activity.getSupportFragmentManager();
            Fragment fragment = manager.findFragmentById(R.id.browser);
            if (fragment instanceof BrowserScreen) {
                setBrowserState(shouldKeepBrowserRunning(this.helper), this.helper);
            }
        }

        private void onStop() {
            this.helper = null;
            this.stateRunnable = null;
        }

        private boolean shouldKeepBrowserRunning(@NonNull TransactionHelper helper) {
            FragmentManager manager = helper.activity.getSupportFragmentManager();
            int entryCount = manager.getBackStackEntryCount();
            for (int i = entryCount - 1; i >= 0; --i) {
                FragmentManager.BackStackEntry entry = manager.getBackStackEntryAt(i);
                if (helper.getEntryType(entry) != EntryData.TYPE_FLOATING) {
                    return false;
                }
            }
            return true;
        }

        private void setBrowserState(boolean isForeground, @NonNull TransactionHelper helper) {
            this.stateRunnable = () -> setBrowserForegroundState(isForeground);

            FragmentAnimationAccessor actor = getTopAnimationAccessibleFragment(helper);
            Animation anim;
            if (actor == null
                    || ((anim = actor.getCustomEnterTransition()) == null)
                    || anim.hasEnded()) {
                executeStateRunnable();
            } else {
                anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        executeStateRunnable();
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
            }
        }

        private void setBrowserForegroundState(boolean isForeground) {
            if (this.helper == null) {
                return;
            }

            FragmentManager manager = this.helper.activity.getSupportFragmentManager();
            BrowserScreen browserFragment = (BrowserScreen) manager.findFragmentById(R.id.browser);
            if (isForeground) {
                browserFragment.goForeground();
            } else {
                browserFragment.goBackground();
            }
        }

        private void executeStateRunnable() {
            if (this.stateRunnable != null) {
                this.stateRunnable.run();
                this.stateRunnable = null;
            }
        }

        @Nullable
        FragmentAnimationAccessor getTopAnimationAccessibleFragment(@NonNull TransactionHelper helper) {
            Fragment top = helper.getLatestCommitFragment();
            if (top != null && top instanceof FragmentAnimationAccessor) {
                return (FragmentAnimationAccessor) top;
            }
            return null;
        }
    }

    static class EntryData {
        /**
         * argument passed to {@link FragmentTransaction#addToBackStack(String)}, pressing back when this
         * type of fragment is in foreground will close the app
         */
        static final int TYPE_ROOT = 0;

        /**
         * argument passed to {@link FragmentTransaction#addToBackStack(String)}, adding fragment of
         * this type will make browser fragment go to background
         */
        static final int TYPE_ATTACHED = 1;

        /**
         * argument passed to {@link FragmentTransaction#addToBackStack(String)}, browsing fragment
         * will still be in foreground after adding this type of fragment.
         */
        static final int TYPE_FLOATING = 2;

        @IntDef({TYPE_ROOT, TYPE_ATTACHED, TYPE_FLOATING})
        @interface EntryType {
        }
    }
}
