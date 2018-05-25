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

import org.mozilla.focus.R;
import org.mozilla.focus.fragment.BrowserFragment;
import org.mozilla.focus.fragment.FirstrunFragment;
import org.mozilla.focus.home.HomeFragment;
import org.mozilla.focus.tabs.tabtray.TabTrayFragment;
import org.mozilla.focus.urlinput.UrlInputFragment;

class MainMediator {

    // For FragmentManager, there is no real top fragment.
    // Instead, we define this sequence for fragments of MainActivity
    // to define that, if there are two visible fragments, which one is top one.
    private final static String[] FRAGMENTS_SEQUENCE = {
            TabTrayFragment.FRAGMENT_TAG,
            UrlInputFragment.FRAGMENT_TAG,
            FirstrunFragment.FRAGMENT_TAG,
            HomeFragment.FRAGMENT_TAG
    };

    /** argument passed to {@link FragmentTransaction#addToBackStack(String)}, pressing back when this
     * type of fragment is in foreground will close the app */
    private static final String TYPE_ROOT = "root";

    /** argument passed to {@link FragmentTransaction#addToBackStack(String)}, adding fragment of
     * this type will make browser fragment go to background */
    private static final String TYPE_ATTACHED = "attached";

    /** argument passed to {@link FragmentTransaction#addToBackStack(String)}, browsing fragment
     * will still be in foreground after adding this type of fragment. */
    private static final String TYPE_FLOATING = "floating";

    private static final String ENTRY_SEPARATOR = "#";

    private final MainActivity activity;

    MainMediator(@NonNull MainActivity activity) {
        this.activity = activity;
        this.activity.getSupportFragmentManager().addOnBackStackChangedListener(new BackStackListener());
    }

    void showHomeScreen(boolean animated, boolean addToBackStack) {
        if (!homeFragmentAtTop()) {
            this.prepareHomeScreen(animated, addToBackStack).commit();
        }
    }

    void showFirstRun() {
        this.prepareFirstRun().commit();
    }

    void showUrlInput(@Nullable String url) {
        final FragmentManager fragmentManager = this.activity.getSupportFragmentManager();
        final Fragment existingFragment = fragmentManager.findFragmentByTag(UrlInputFragment.FRAGMENT_TAG);
        if (existingFragment != null && existingFragment.isAdded() && !existingFragment.isRemoving()) {
            // We are already showing an URL input fragment. This might have been a double click on the
            // fake URL bar. Just ignore it.
            return;
        }

        String parent = homeFragmentAtTop() ? HomeFragment.FRAGMENT_TAG : BrowserFragment.FRAGMENT_TAG;
        this.prepareUrlInput(url, parent)
                .addToBackStack(makeEntryName(UrlInputFragment.FRAGMENT_TAG, TYPE_FLOATING))
                .commit();
    }

    void dismissUrlInput() {
        final Fragment top = getTopFragment();
        if (top != null && UrlInputFragment.FRAGMENT_TAG.equals(top.getTag())) {
            this.activity.onBackPressed();
        }
    }

    boolean shouldFinish() {
        FragmentManager manager = activity.getSupportFragmentManager();
        int entryCount = manager.getBackStackEntryCount();
        if (entryCount == 0) {
            return true;
        }

        FragmentManager.BackStackEntry lastEntry = manager.getBackStackEntryAt(entryCount - 1);
        return TYPE_ROOT.equals(getEntryType(lastEntry));
    }

    private Fragment getTopFragment() {
        final FragmentManager fragmentManager = this.activity.getSupportFragmentManager();
        for (final String tag : FRAGMENTS_SEQUENCE) {
            final Fragment fragment = fragmentManager.findFragmentByTag(tag);
            if (fragment != null && fragment.isVisible()) {
                return fragment;
            }
        }
        return null;
    }

    private FragmentTransaction prepareFirstRun() {
        final FragmentManager fragmentManager = this.activity.getSupportFragmentManager();
        final FirstrunFragment fragment = this.activity.createFirstRunFragment();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (fragmentManager.findFragmentByTag(FirstrunFragment.FRAGMENT_TAG) == null) {
            transaction.replace(R.id.container, fragment, FirstrunFragment.FRAGMENT_TAG)
                    .addToBackStack(makeEntryName(FirstrunFragment.FRAGMENT_TAG, TYPE_ROOT));
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
            transaction.addToBackStack(makeEntryName(HomeFragment.FRAGMENT_TAG, TYPE_ATTACHED));
        } else {
            transaction.replace(R.id.container, fragment, HomeFragment.FRAGMENT_TAG);
            transaction.addToBackStack(makeEntryName(HomeFragment.FRAGMENT_TAG, TYPE_ROOT));
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

    /**
     * get HomeFragment if it's Top Fragment
     */
    private HomeFragment getTopHomeFragment() {
        final Fragment topFragment = getTopFragment();
        if (topFragment != null && HomeFragment.FRAGMENT_TAG.equals(topFragment.getTag())) {
            return (HomeFragment) topFragment;
        }
        return null;
    }

    private boolean homeFragmentAtTop() {
        return getTopHomeFragment() != null;
    }

    private String makeEntryName(String tag, String type) {
        return tag + ENTRY_SEPARATOR + type;
    }

    String getEntryTag(FragmentManager.BackStackEntry entry) {
        String split[] = entry.getName().split(ENTRY_SEPARATOR);
        if (split.length != 2) {
            throw new RuntimeException("illegal name passed to addToBackStack(): " + entry.getName());
        }
        return split[0];
    }

    private String getEntryType(FragmentManager.BackStackEntry entry) {
        String split[] = entry.getName().split(ENTRY_SEPARATOR);
        if (split.length != 2) {
            throw new RuntimeException("illegal name passed to addToBackStack(): " + entry.getName());
        }
        return split[1];
    }

    private class BackStackListener implements FragmentManager.OnBackStackChangedListener {

        @Override
        public void onBackStackChanged() {
            FragmentManager manager = activity.getSupportFragmentManager();
            int entryCount = manager.getBackStackEntryCount();

            boolean isBrowserForeground = true;
            for (int i = 0; i < entryCount; ++i) {
                FragmentManager.BackStackEntry entry = manager.getBackStackEntryAt(i);
                if (!TextUtils.equals(getEntryType(entry), TYPE_FLOATING)) {
                    isBrowserForeground = false;
                    break;
                }
            }

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
}
