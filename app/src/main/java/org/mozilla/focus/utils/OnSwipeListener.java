/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

public interface OnSwipeListener {
    default void onSwipeRight() {
    }

    default void onSwipeLeft() {
    }

    default void onSwipeUp() {
    }

    default void onSwipeDown() {
    }

    default void onLongPress() {
    }

    default boolean onDoubleTap() {
        return false;
    }

    default boolean onSingleTapConfirmed() {
        return false;
    }

}
