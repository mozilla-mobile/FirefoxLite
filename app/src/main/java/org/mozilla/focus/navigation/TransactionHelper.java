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
import android.util.Log;
import android.view.animation.Animation;

import org.mozilla.focus.BuildConfig;
import org.mozilla.focus.R;
import org.mozilla.focus.activity.MainActivity;
import org.mozilla.focus.fragment.BrowserFragment;
import org.mozilla.focus.fragment.FirstrunFragment;
import org.mozilla.focus.home.HomeFragment;
import org.mozilla.focus.urlinput.UrlInputFragment;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

class TransactionHelper implements DefaultLifecycleObserver {
    private static final String TAG = "TransactionHelper";

    private EntryDataMap entryDataMap = new EntryDataMap();

    private final MainActivity activity;
    private final BackStackListener backStackListener;

    TransactionHelper(@NonNull MainActivity activity) {
        this.activity = activity;
        this.backStackListener = new BackStackListener(this);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        this.activity.getSupportFragmentManager()
                .addOnBackStackChangedListener(this.backStackListener);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        this.activity.getSupportFragmentManager()
                .removeOnBackStackChangedListener(this.backStackListener);
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
        final Fragment existingFragment = fragmentManager.findFragmentByTag(UrlInputFragment.FRAGMENT_TAG);
        if (existingFragment != null && existingFragment.isAdded() && !existingFragment.isRemoving()) {
            // We are already showing an URL input fragment. This might have been a double click on the
            // fake URL bar. Just ignore it.
            return;
        }

        this.prepareUrlInput(url, sourceFragment)
                .addToBackStack(this.entryDataMap.add(UrlInputFragment.FRAGMENT_TAG, EntryData.TYPE_FLOATING))
                .commit();
    }

    void dismissUrlInput() {
        this.activity.onBackPressed();
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

    void updateForegroundType(@EntryData.EntryType int type) {
        FragmentManager manager = activity.getSupportFragmentManager();
        int size = manager.getBackStackEntryCount();
        if (size == 0) {
            return;
        }
        FragmentManager.BackStackEntry entry = manager.getBackStackEntryAt(size - 1);
        if (this.entryDataMap.get(entry).type != type) {
            this.entryDataMap.get(entry).type = type;
        }
    }

    void popAllScreens() {
        popScreensUntil(null);
    }

    boolean popScreensUntil(@Nullable String targetEntryName) {
        boolean clearAll = (targetEntryName == null);
        FragmentManager manager = this.activity.getSupportFragmentManager();
        int entryCount = manager.getBackStackEntryCount();
        boolean found = false;
        while (entryCount > 0) {
            FragmentManager.BackStackEntry entry = manager.getBackStackEntryAt(entryCount - 1);
            if (!clearAll && TextUtils.equals(targetEntryName, getEntryTag(entry))) {
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
        final FirstrunFragment fragment = this.activity.createFirstRunFragment();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (fragmentManager.findFragmentByTag(FirstrunFragment.FRAGMENT_TAG) == null) {
            transaction.replace(R.id.container, fragment, FirstrunFragment.FRAGMENT_TAG)
                    .addToBackStack(this.entryDataMap.add(FirstrunFragment.FRAGMENT_TAG, EntryData.TYPE_ROOT));
        }

        return transaction;
    }

    private FragmentTransaction prepareHomeScreen(boolean animated, @EntryData.EntryType int type) {
        final FragmentManager fragmentManager = this.activity.getSupportFragmentManager();
        final HomeFragment fragment = this.activity.createHomeFragment();

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        int enterAnim = animated ? R.anim.tab_transition_fade_in : 0;
        int exitAnim = (type == EntryData.TYPE_ROOT) ? 0 : R.anim.tab_transition_fade_out;
        transaction.setCustomAnimations(enterAnim, 0, 0, exitAnim);

        transaction.add(R.id.container, fragment, HomeFragment.FRAGMENT_TAG);
        transaction.addToBackStack(this.entryDataMap.add(HomeFragment.FRAGMENT_TAG, type));

        return transaction;
    }

    private FragmentTransaction prepareUrlInput(@Nullable String url, String parentFragmentTag) {
        final FragmentManager fragmentManager = this.activity.getSupportFragmentManager();
        final UrlInputFragment urlFragment = this.activity.createUrlInputFragment(url, parentFragmentTag);
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.container, urlFragment, UrlInputFragment.FRAGMENT_TAG);
        return transaction;
    }

    void toggleFakeUrlInput(boolean visible) {
        final FragmentManager fragmentManager = this.activity.getSupportFragmentManager();
        final HomeFragment homeFragment =
                (HomeFragment) fragmentManager.findFragmentByTag(HomeFragment.FRAGMENT_TAG);
        if (homeFragment != null && homeFragment.isVisible()) {
            homeFragment.toggleFakeUrlInput(visible);
        }
    }

    private String getFragmentTag(int backStackIndex) {
        FragmentManager manager = this.activity.getSupportFragmentManager();
        return getEntryTag(manager.getBackStackEntryAt(backStackIndex));
    }

    private String getEntryTag(FragmentManager.BackStackEntry entry) {
        return this.entryDataMap.get(entry).tag;
    }

    private @EntryData.EntryType int getEntryType(FragmentManager.BackStackEntry entry) {
        return this.entryDataMap.get(entry).type;
    }

    private boolean isStateSaved() {
        FragmentManager manager = this.activity.getSupportFragmentManager();
        return manager == null || manager.isStateSaved();
    }

    private static class BackStackListener implements FragmentManager.OnBackStackChangedListener {
        private Runnable stateRunnable;
        private WeakReference<TransactionHelper> helperRef;

        BackStackListener(TransactionHelper helper) {
            this.helperRef = new WeakReference<>(helper);
        }

        @Override
        public void onBackStackChanged() {
            TransactionHelper helper = this.helperRef.get();
            if (helper == null) {
                return;
            }

            FragmentManager manager = helper.activity.getSupportFragmentManager();
            helper.entryDataMap.refreshData(manager);

            Fragment fragment = manager.findFragmentById(R.id.browser);
            if (fragment instanceof BrowserFragment) {
                setBrowserState(shouldKeepBrowserRunning(manager));
            }
        }

        private boolean shouldKeepBrowserRunning(FragmentManager manager) {
            TransactionHelper helper = this.helperRef.get();
            if (helper == null) {
                return false;
            }

            int entryCount = manager.getBackStackEntryCount();
            for (int i = entryCount - 1; i >= 0; --i) {
                FragmentManager.BackStackEntry entry = manager.getBackStackEntryAt(i);
                if (helper.getEntryType(entry) != EntryData.TYPE_FLOATING) {
                    return false;
                }
            }
            return true;
        }

        private void setBrowserState(boolean isForeground) {
            this.stateRunnable = () -> {
                TransactionHelper helper = this.helperRef.get();
                if (helper == null) {
                    return;
                }

                FragmentManager manager = helper.activity.getSupportFragmentManager();
                BrowserFragment browserFragment = (BrowserFragment) manager.findFragmentById(R.id.browser);
                if (isForeground) {
                    browserFragment.goForeground();
                } else {
                    browserFragment.goBackground();
                }
            };

            FragmentAnimationAccessor actor = getTopAnimationAccessibleFragment();
            Animation anim;
            if (actor == null
                    || ((anim = actor.getCustomEnterTransition()) == null)
                    || anim.hasEnded()) {
                executeStateRunnable();
            } else {
                anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {}

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        executeStateRunnable();
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });
            }
        }

        private void executeStateRunnable() {
            if (this.stateRunnable != null) {
                this.stateRunnable.run();
                this.stateRunnable = null;
            }
        }

        @Nullable
        FragmentAnimationAccessor getTopAnimationAccessibleFragment() {
            TransactionHelper helper = this.helperRef.get();
            if (helper == null) {
                return null;
            }

            Fragment top = helper.getLatestCommitFragment();
            if (top != null && top instanceof FragmentAnimationAccessor) {
                return (FragmentAnimationAccessor) top;
            }
            return null;
        }
    }

    static class EntryData {
        /** argument passed to {@link FragmentTransaction#addToBackStack(String)}, pressing back when this
         * type of fragment is in foreground will close the app */
        static final int TYPE_ROOT = 0;

        /** argument passed to {@link FragmentTransaction#addToBackStack(String)}, adding fragment of
         * this type will make browser fragment go to background */
        static final int TYPE_ATTACHED = 1;

        /** argument passed to {@link FragmentTransaction#addToBackStack(String)}, browsing fragment
         * will still be in foreground after adding this type of fragment. */
        static final int TYPE_FLOATING = 2;

        @IntDef({TYPE_ROOT, TYPE_ATTACHED, TYPE_FLOATING})
        @interface EntryType {}

        String tag;
        @EntryType int type;
    }

    private static class EntryDataMap {
        private HashMap<String, EntryData> dataMap = new HashMap<>();

        /**
         * @return an unique string to be used to retrieve related info about this transaction from
         * the data map
         */
        String add(String tag, @EntryData.EntryType int type) {
            EntryData data = new EntryData();
            data.tag = tag;
            data.type = type;

            String key = Integer.toHexString(data.hashCode());
            this.dataMap.put(key, data);

            logData("addNewEntry");
            return key;
        }

        EntryData get(FragmentManager.BackStackEntry entry) {
            return dataMap.get(entry.getName());
        }

        void refreshData(FragmentManager manager) {
            HashMap<String, EntryData> newMap = new HashMap<>();
            int size = manager.getBackStackEntryCount();
            for (int i = 0; i < size; ++i) {
                String key = manager.getBackStackEntryAt(i).getName();
                newMap.put(key, this.dataMap.get(key));
            }
            this.dataMap = newMap;
            logData("purge");
        }

        private void logData(String action) {
            if (!BuildConfig.DEBUG) {
                return;
            }

            Log.d(TAG, "action: " + action);
            if (this.dataMap.isEmpty()) {
                Log.d(TAG, "\tempty");
            } else {
                for (Map.Entry<String, EntryData> entry : this.dataMap.entrySet()) {
                    EntryData entryData = entry.getValue();
                    Log.d(TAG, "\t" + entry.getKey() + ":(" + entryData.tag
                            + ", " + entryData.type + ")");
                }
            }
        }
    }
}
