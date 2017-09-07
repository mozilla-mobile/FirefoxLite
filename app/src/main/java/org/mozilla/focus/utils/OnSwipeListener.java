package org.mozilla.focus.utils;

public interface OnSwipeListener {
    void onSwipeRight();

    void onSwipeLeft();

    void onSwipeUp();

    void onSwipeDown();

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
    }
}
