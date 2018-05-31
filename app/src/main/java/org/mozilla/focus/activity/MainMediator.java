/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.Log;

import org.mozilla.focus.BuildConfig;
import org.mozilla.focus.R;
import org.mozilla.focus.fragment.BrowserFragment;
import org.mozilla.focus.fragment.FirstrunFragment;
import org.mozilla.focus.home.HomeFragment;
import org.mozilla.focus.urlinput.UrlInputFragment;

import java.util.HashMap;

class MainMediator {
    private static final String TAG = "MainMediator";

    private EntryDataSet entryDataSet = new EntryDataSet();

    private final MainActivity activity;

    MainMediator(@NonNull MainActivity activity) {
        this.activity = activity;
        this.activity.getSupportFragmentManager().addOnBackStackChangedListener(new BackStackListener());
    }

    void showHomeScreen(boolean animated, boolean addToBackStack) {
        this.prepareHomeScreen(animated, addToBackStack).commit();
    }

    void showFirstRun() {
        this.prepareFirstRun().commit();
    }

    void showUrlInput(@Nullable String url, String sourceFragment) {
        final FragmentManager fragmentManager = this.activity.getSupportFragmentManager();
        final Fragment existingFragment = fragmentManager.findFragmentByTag(UrlInputFragment.FRAGMENT_TAG);
        if (existingFragment != null && existingFragment.isAdded() && !existingFragment.isRemoving()) {
            // We are already showing an URL input fragment. This might have been a double click on the
            // fake URL bar. Just ignore it.
            return;
        }

        this.prepareUrlInput(url, sourceFragment)
                .addToBackStack(entryDataSet.add(UrlInputFragment.FRAGMENT_TAG, EntryData.TYPE_FLOATING))
                .commit();
    }

    void dismissUrlInput() {
        this.activity.onBackPressed();
    }

    boolean shouldFinish() {
        FragmentManager manager = activity.getSupportFragmentManager();
        int entryCount = manager.getBackStackEntryCount();
        if (entryCount == 0) {
            return true;
        }

        FragmentManager.BackStackEntry lastEntry = manager.getBackStackEntryAt(entryCount - 1);
        return EntryData.TYPE_ROOT.equals(getEntryType(lastEntry));
    }

    private FragmentTransaction prepareFirstRun() {
        final FragmentManager fragmentManager = this.activity.getSupportFragmentManager();
        final FirstrunFragment fragment = this.activity.createFirstRunFragment();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (fragmentManager.findFragmentByTag(FirstrunFragment.FRAGMENT_TAG) == null) {
            transaction.replace(R.id.container, fragment, FirstrunFragment.FRAGMENT_TAG)
                    .addToBackStack(entryDataSet.add(FirstrunFragment.FRAGMENT_TAG, EntryData.TYPE_ROOT));
        }

        return transaction;
    }

    private FragmentTransaction prepareHomeScreen(boolean animated, boolean addToBackStack) {
        final FragmentManager fragmentManager = this.activity.getSupportFragmentManager();
        final HomeFragment fragment = this.activity.createHomeFragment();

        // Two different ways to add HomeFragment.
        // 1. If Fragments stack is empty, or only first-run - add HomeFragment to bottom of stack.
        // 2. If we are browsing web pages and launch HomeFragment, hoist HomeFragment from bottom.
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (animated) {
            transaction.setCustomAnimations(R.anim.tab_transition_fade_in, R.anim.tab_transition_fade_out,
                    R.anim.tab_transition_fade_in, R.anim.tab_transition_fade_out);
        } else {
            transaction.setCustomAnimations(0, 0, R.anim.tab_transition_fade_in,
                    R.anim.tab_transition_fade_out);
        }

        if (addToBackStack) {
            transaction.add(R.id.container, fragment, HomeFragment.FRAGMENT_TAG);
            transaction.addToBackStack(entryDataSet.add(HomeFragment.FRAGMENT_TAG, EntryData.TYPE_ATTACHED));
        } else {
            transaction.replace(R.id.container, fragment, HomeFragment.FRAGMENT_TAG);
            transaction.addToBackStack(entryDataSet.add(HomeFragment.FRAGMENT_TAG, EntryData.TYPE_ROOT));
        }

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

    String getEntryTag(FragmentManager.BackStackEntry entry) {
        return entryDataSet.get(entry).tag;
    }

    private String getEntryType(FragmentManager.BackStackEntry entry) {
        return entryDataSet.get(entry).type;
    }

    private class BackStackListener implements FragmentManager.OnBackStackChangedListener {

        @Override
        public void onBackStackChanged() {
            FragmentManager manager = activity.getSupportFragmentManager();
            int entryCount = manager.getBackStackEntryCount();

            boolean isBrowserForeground = true;
            for (int i = 0; i < entryCount; ++i) {
                FragmentManager.BackStackEntry entry = manager.getBackStackEntryAt(i);
                if (!TextUtils.equals(getEntryType(entry), EntryData.TYPE_FLOATING)) {
                    isBrowserForeground = false;
                    break;
                }
            }

            entryDataSet.onBackStackChanged(manager);
            entryDataSet.purge();

            Fragment fragment = manager.findFragmentById(R.id.browser);
            if (fragment instanceof BrowserFragment) {
                if (isBrowserForeground) {
                    ((BrowserFragment) fragment).goForeground();
                } else {
                    ((BrowserFragment) fragment).goBackground();
                }
            }
        }
    }

    private static class EntryData {
        /** argument passed to {@link FragmentTransaction#addToBackStack(String)}, pressing back when this
         * type of fragment is in foreground will close the app */
        private static final String TYPE_ROOT = "root";

        /** argument passed to {@link FragmentTransaction#addToBackStack(String)}, adding fragment of
         * this type will make browser fragment go to background */
        private static final String TYPE_ATTACHED = "attached";

        /** argument passed to {@link FragmentTransaction#addToBackStack(String)}, browsing fragment
         * will still be in foreground after adding this type of fragment. */
        private static final String TYPE_FLOATING = "floating";

        String tag;
        String type;
    }

    private static class EntryDataSet {
        private HashMap<String, EntryData> data = new HashMap<>();
        private HashMap<String, EntryData> backStackRecords = new HashMap<>();

        String add(String tag, String type) {
            EntryData data = new EntryData();
            data.tag = tag;
            data.type = type;

            String key = Integer.toHexString(data.hashCode());
            this.data.put(key, data);

            logData("addNewEntry");
            return key;
        }

        EntryData get(FragmentManager.BackStackEntry entry) {
            return data.get(entry.getName());
        }

        void onBackStackChanged(FragmentManager manager) {
            backStackRecords.clear();
            int size = manager.getBackStackEntryCount();
            for (int i = 0; i < size; ++i) {
                FragmentManager.BackStackEntry entry = manager.getBackStackEntryAt(i);
                EntryData data = this.data.get(entry.getName());
                backStackRecords.put(entry.getName(), data);
            }
        }

        private void purge() {
            HashMap<String, EntryData> tmp = data;
            data = backStackRecords;
            backStackRecords = tmp;
            logData("purge");
        }

        private void logData(String action) {
            if (!BuildConfig.DEBUG) {
                return;
            }

            Log.d(TAG, "action: " + action);
            if (this.data.isEmpty()) {
                Log.d(TAG, "\tempty");
            } else {
                for (String key : this.data.keySet()) {
                    Log.d(TAG, "\t" + key + ":(" + this.data.get(key).tag
                            + ", " + this.data.get(key).type + ")");
                }
            }
        }
    }
}
