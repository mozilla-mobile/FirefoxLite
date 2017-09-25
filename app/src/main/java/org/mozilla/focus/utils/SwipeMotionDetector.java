/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class SwipeMotionDetector implements View.OnTouchListener {

    private GestureDetector gestureDetector;
    private OnSwipeListener onSwipeListener;

    public SwipeMotionDetector(Context c, OnSwipeListener onSwipeListener) {
        gestureDetector = new GestureDetector(c, new GestureListener());
        this.onSwipeListener = onSwipeListener;
    }

    public boolean onTouch(final View view, final MotionEvent motionEvent) {
        return gestureDetector.onTouchEvent(motionEvent);
    }

    private final class GestureListener extends GestureDetector.SimpleOnGestureListener {

        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            boolean result = false;
            float diffY = e2.getY() - e1.getY();
            float diffX = e2.getX() - e1.getX();
            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        if(onSwipeListener != null) {
                            onSwipeListener.onSwipeRight();
                        }
                    } else {
                        if(onSwipeListener != null) {
                            onSwipeListener.onSwipeLeft();
                        }
                    }
                }
            } else {
                if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        if(onSwipeListener != null) {
                            onSwipeListener.onSwipeDown();
                        }
                    } else {
                        if (onSwipeListener != null) {
                            onSwipeListener.onSwipeUp();
                        }

                    }
                }
            }
            return result;
        }
    }
}
