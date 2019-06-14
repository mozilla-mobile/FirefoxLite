package org.mozilla.focus.helper;

import androidx.test.espresso.IdlingResource;
import androidx.viewpager.widget.ViewPager;

/**
 * An IdlingResource implementation that waits until the ViewPager is not loading anymore.
 * Only after loading has completed further actions will be performed.
 */
public class ViewPagerIdlingResource implements IdlingResource {

    private boolean mIdle = true; // Default to idle since we can't query the scroll state.

    private ResourceCallback mResourceCallback;

    public ViewPagerIdlingResource(ViewPager viewPager) {
        viewPager.addOnPageChangeListener(new ViewPagerListener());

    }

    @Override
    public String getName() {
        return ViewPagerIdlingResource.class.getSimpleName();
    }

    @Override
    public boolean isIdleNow() {
        return mIdle;
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
        mResourceCallback = resourceCallback;
    }

    private class ViewPagerListener extends ViewPager.SimpleOnPageChangeListener {

        @Override
        public void onPageScrollStateChanged(int state) {
            mIdle = (state == ViewPager.SCROLL_STATE_IDLE
                    // Treat dragging as idle, or Espresso will block itself when swiping.
                    || state == ViewPager.SCROLL_STATE_DRAGGING);
            if (mIdle && mResourceCallback != null) {
                mResourceCallback.onTransitionToIdle();
            }
        }
    }
}