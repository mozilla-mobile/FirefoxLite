/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

public interface OnSwipeListener {
    void onSwipeRight();

    void onSwipeLeft();

    void onSwipeUp();

    void onSwipeDown();

    void onLongPress();

    boolean onDoubleTap();

    class OnSwipeListenerAdapter implements OnSwipeListener {

        @Override
        public void onSwipeRight() {

        }

        @Override
        public void onSwipeLeft() {

        }

        @Override
        public void onSwipeUp() {

        }

        @Override
        public void onSwipeDown() {

        }

        @Override
        public void onLongPress() {
        }

        @Override
        public boolean onDoubleTap() {
            return false;
        }
    }
}
