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
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

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

    private final MainActivity activity;

    MainMediator(@NonNull MainActivity activity) {
        this.activity = activity;
    }

    /**
     * Show landing home screen when app is launched
     */
    void showHomeScreen() {
        this.showHomeScreen(false, false);
    }

    void showHomeScreen(boolean animated, boolean addToBackStack) {
        if (getTopHomeFragment() == null) {
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

        String parent = isHomeFragmentVisible() ? HomeFragment.FRAGMENT_TAG : BrowserFragment.FRAGMENT_TAG;
        this.prepareUrlInput(url, parent).addToBackStack(UrlInputFragment.FRAGMENT_TAG).commit();
    }

    void dismissUrlInput() {
        final Fragment top = getTopFragment();
        if (top != null && UrlInputFragment.FRAGMENT_TAG.equals(top.getTag())) {
            this.activity.onBackPressed();
        }
    }

    private void clearBackStack(FragmentManager fm) {
        fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    private void clearAllFragmentImmediate() {
        final FragmentManager fragmentMgr = this.activity.getSupportFragmentManager();
        final Fragment urlInputFrg = fragmentMgr.findFragmentByTag(UrlInputFragment.FRAGMENT_TAG);
        final Fragment homeFrg = fragmentMgr.findFragmentByTag(HomeFragment.FRAGMENT_TAG);

        // If UrlInputFragment exists, remove it and clear its transaction from back stack
        FragmentTransaction clear = fragmentMgr.beginTransaction();
        clear = (urlInputFrg == null) ? clear : clear.remove(urlInputFrg);
        clear = (homeFrg == null) ? clear : clear.remove(homeFrg);
        clear.commit();

        clearBackStack(fragmentMgr);
    }

    void clearAllFragment(boolean animate) {
        final FragmentManager manager = this.activity.getSupportFragmentManager();
        final Fragment homeFragment = manager.findFragmentByTag(HomeFragment.FRAGMENT_TAG);
        if (!animate || homeFragment == null) {
            clearAllFragmentImmediate();
            return;
        }

        fadeOutFragment(homeFragment, new Runnable() {
            @Override
            public void run() {
                clearAllFragmentImmediate();
            }
        });
    }

    private void fadeOutFragment(Fragment fragment, @Nullable final Runnable onAnimationEndCallback) {
        View view = fragment.getView();
        if (view == null) {
            if (onAnimationEndCallback != null) {
                onAnimationEndCallback.run();
            }
            return;
        }

        Animation fadeOut = AnimationUtils.loadAnimation(view.getContext(),
                R.anim.tab_transition_fade_out);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (onAnimationEndCallback != null) {
                    onAnimationEndCallback.run();
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        view.startAnimation(fadeOut);
    }

    void onFragmentStarted(@NonNull String tag) {
        if (UrlInputFragment.FRAGMENT_TAG.equals(tag)) {
            toggleFakeUrlInput(false);
        }
    }

    void onFragmentStopped(@NonNull String tag) {
        if (UrlInputFragment.FRAGMENT_TAG.equals(tag)) {
            toggleFakeUrlInput(true);
        }
    }

    Fragment getTopFragment() {
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
            transaction.replace(R.id.container,
                    fragment,
                    FirstrunFragment.FRAGMENT_TAG);
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
            transaction.addToBackStack(HomeFragment.FRAGMENT_TAG);
        } else {
            transaction.replace(R.id.container, fragment, HomeFragment.FRAGMENT_TAG);
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

    private void toggleFakeUrlInput(boolean visible) {
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
    Fragment getTopHomeFragment() {
        final Fragment topFragment = getTopFragment();
        if (topFragment != null && HomeFragment.FRAGMENT_TAG.equals(topFragment.getTag())) {
            return topFragment;
        }
        return null;
    }

    private boolean isHomeFragmentVisible() {
        final FragmentManager fragmentManager = this.activity.getSupportFragmentManager();
        final Fragment fragment = fragmentManager.findFragmentByTag(HomeFragment.FRAGMENT_TAG);
        return fragment != null && fragment.isVisible();
    }
}
