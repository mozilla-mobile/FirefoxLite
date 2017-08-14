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

import org.mozilla.focus.R;
import org.mozilla.focus.fragment.BrowserFragment;
import org.mozilla.focus.fragment.FirstrunFragment;
import org.mozilla.focus.home.HomeFragment;
import org.mozilla.focus.urlinput.UrlInputFragment;
import org.mozilla.focus.widget.BackKeyHandleable;

public class MainMediator {

    // For FragmentManager, there is no real top fragment.
    // Instead, we define this sequence for fragments of MainActivity
    // to define that, if there are two visible fragments, which one is top one.
    private final static String[] FRAGMENTS_SEQUENCE = {
            UrlInputFragment.FRAGMENT_TAG,
            BrowserFragment.FRAGMENT_TAG,
            FirstrunFragment.FRAGMENT_TAG,
            HomeFragment.FRAGMENT_TAG
    };

    // To indicate the Transaction is to hoist home fragment to user visible area.
    // This transaction could be wiped if user try to see browser fragment again.
    private final static String HOIST_HOME_FRAGMENT = "_hoist_home_fragment_";

    private final MainActivity activity;

    public MainMediator(@NonNull MainActivity activity) {
        this.activity = activity;
    }

    public void showHomeScreen() {
        this.prepareHomeScreen().commit();
    }


    public void showFirstRun() {
        this.prepareFirstRun().commit();
    }

    public void showUrlInput(@Nullable String url) {
        final FragmentManager fragmentManager = this.activity.getSupportFragmentManager();
        final Fragment existingFragment = fragmentManager.findFragmentByTag(UrlInputFragment.FRAGMENT_TAG);
        if (existingFragment != null && existingFragment.isAdded() && !existingFragment.isRemoving()) {
            // We are already showing an URL input fragment. This might have been a double click on the
            // fake URL bar. Just ignore it.
            return;
        }

        this.prepareUrlInput(url).addToBackStack(UrlInputFragment.FRAGMENT_TAG).commit();
    }

    public void showBrowserScreen(@Nullable String url) {
        final FragmentManager fragmentMgr = this.activity.getSupportFragmentManager();
        final Fragment urlInputFrg = fragmentMgr.findFragmentByTag(UrlInputFragment.FRAGMENT_TAG);

        // If UrlInputFragment exists, remove it and clear its transaction from back stack
        FragmentTransaction clear = fragmentMgr.beginTransaction();
        clear = (urlInputFrg == null) ? clear : clear.remove(urlInputFrg);
        clear.commit();
        fragmentMgr.popBackStackImmediate(UrlInputFragment.FRAGMENT_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        // To hide HomeFragment silently, herr to wipe the transaction from back stack
        fragmentMgr.popBackStackImmediate(HOIST_HOME_FRAGMENT, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        FragmentTransaction trans = this.prepareBrowsing(url);
        trans.commit();

        this.activity.sendBrowsingTelemetry();
    }

    public void dismissUrlInput() {
        final Fragment top = getTopFragment();
        if (UrlInputFragment.FRAGMENT_TAG.equals(top.getTag())) {
            this.activity.onBackPressed();
        }
    }

    public boolean handleBackKey() {
        final Fragment topFrg = getTopFragment();
        return (topFrg instanceof BackKeyHandleable) && ((BackKeyHandleable) topFrg).onBackPressed();
    }

    public void onFragmentStarted(@NonNull String tag) {
        if (UrlInputFragment.FRAGMENT_TAG.equals(tag)) {
            toggleFakeUrlInput(false);
        }
    }

    public void onFragmentStopped(@NonNull String tag) {
        if (UrlInputFragment.FRAGMENT_TAG.equals(tag)) {
            toggleFakeUrlInput(true);
        }
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

    private FragmentTransaction prepareBrowsing(@Nullable String url) {
        final FragmentManager fragmentMgr = this.activity.getSupportFragmentManager();
        FragmentTransaction transaction = fragmentMgr.beginTransaction();

        final BrowserFragment browserFrg = (BrowserFragment) fragmentMgr
                .findFragmentByTag(BrowserFragment.FRAGMENT_TAG);

        if (browserFrg == null) {
            final Fragment freshFragment = this.activity.createBrowserFragment(url);
            transaction.add(R.id.container, freshFragment, BrowserFragment.FRAGMENT_TAG)
                    .addToBackStack(null);
        } else {
            // Reuse existing visible fragment - in this case we know the user is already browsing.
            // The fragment might exist if we "erased" a browsing session, hence we need to check
            // for visibility in addition to existence.
            browserFrg.loadUrl(url);

            if (!browserFrg.isVisible()) {
                transaction.replace(R.id.container, browserFrg, BrowserFragment.FRAGMENT_TAG);
            }
        }
        return transaction;
    }

    private FragmentTransaction prepareFirstRun() {
        final FragmentManager fragmentManager = this.activity.getSupportFragmentManager();
        final FirstrunFragment fragment = this.activity.createFirstRunFragment();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (fragmentManager.findFragmentByTag(FirstrunFragment.FRAGMENT_TAG) == null) {
            transaction.replace(R.id.container,
                    fragment,
                    FirstrunFragment.FRAGMENT_TAG);
        }

        return transaction;
    }

    private FragmentTransaction prepareHomeScreen() {
        final FragmentManager fragmentManager = this.activity.getSupportFragmentManager();
        final HomeFragment fragment = this.activity.createHomeFragment();

        // Two different ways to add HomeFragment.
        // 1. If Fragments stack is empty, or only first-run - add HomeFragment to bottom of stack.
        // 2. If we are browsing web pages and launch HomeFragment, hoist HomeFragment from bottom.
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        final Fragment topFragment = getTopFragment();
        if ((topFragment == null) || FirstrunFragment.FRAGMENT_TAG.equals(topFragment.getTag())) {
            transaction.replace(R.id.container, fragment, HomeFragment.FRAGMENT_TAG);
        } else {
            // hoist home fragment and add to back stack, so back-key still works.
            transaction.replace(R.id.container, fragment, HomeFragment.FRAGMENT_TAG)
                    .addToBackStack(HOIST_HOME_FRAGMENT);
        }
        return transaction;
    }

    private FragmentTransaction prepareUrlInput(@Nullable String url) {
        final FragmentManager fragmentManager = this.activity.getSupportFragmentManager();
        final UrlInputFragment urlFragment = this.activity.createUrlInputFragment(url);
        FragmentTransaction transaction = fragmentManager.beginTransaction()
                .add(R.id.container, urlFragment, UrlInputFragment.FRAGMENT_TAG);
        return transaction;
    }

    private void toggleFakeUrlInput(boolean visible) {
        final FragmentManager fragmentManager = this.activity.getSupportFragmentManager();
        final HomeFragment homeFragment =
                (HomeFragment) fragmentManager.findFragmentByTag(HomeFragment.FRAGMENT_TAG);
        if (homeFragment != null) {
            homeFragment.toggleFakeUrlInput(visible);
        }
    }
}
