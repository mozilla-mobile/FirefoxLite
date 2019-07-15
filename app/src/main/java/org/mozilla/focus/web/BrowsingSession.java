/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.web;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

/**
 * A global object keeping the state of the current browsing session.
 * <p>
 * For now it only tracks whether a browsing session is active or not.
 */
public class BrowsingSession {
    private static BrowsingSession instance;

    public static synchronized BrowsingSession getInstance() {
        if (instance == null) {
            instance = new BrowsingSession();
        }
        return instance;
    }

    private int blockedTrackers;
    private MutableLiveData<Integer> blockedCountData = new MutableLiveData<>();

    private BrowsingSession() {
    }

    public void countBlockedTracker() {
        blockedTrackers++;
        blockedCountData.postValue(blockedTrackers);
    }

    public void setBlockedTrackerCount(int count) {
        blockedTrackers = count;
        blockedCountData.postValue(count);
    }

    public void resetTrackerCount() {
        blockedTrackers = 0;
        blockedCountData.postValue(blockedTrackers);
    }

    public LiveData<Integer> getBlockedTrackerCount() {
        return Transformations.map(blockedCountData, input -> input);
    }
}
