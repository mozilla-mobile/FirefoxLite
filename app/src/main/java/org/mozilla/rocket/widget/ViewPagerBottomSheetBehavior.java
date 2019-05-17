package org.mozilla.rocket.widget;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;


/**
 * Override {@link #findScrollingChild(View)} to support {@link ViewPager}'s nested scrolling.
 *
 */
public class ViewPagerBottomSheetBehavior<V extends View> extends BottomSheetBehavior<V> {

    public ViewPagerBottomSheetBehavior() {
    }

    public ViewPagerBottomSheetBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    View findScrollingChild(View view) {
        if (view == null || ViewCompat.isNestedScrollingEnabled(view)) {
            return view;
        }

        // Previous approach uses DFS to find the scrolling child. Now we consider the current
        // selected page for ViewPager first, so the second page could still gets touch event
        if (view instanceof ViewPager) {
            ViewPager viewPager = (ViewPager) view;
            View currentViewPagerChild = viewPager.getFocusedChild();
            View scrollingChild = findScrollingChild(currentViewPagerChild);
            if (scrollingChild != null) {
                return scrollingChild;
            }
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0, count = group.getChildCount(); i < count; i++) {
                View scrollingChild = findScrollingChild(group.getChildAt(i));
                if (scrollingChild != null) {
                    return scrollingChild;
                }
            }
        }
        return null;
    }

    /**
     * A utility function to get the {@link ViewPagerBottomSheetBehavior} associated with the {@code view}.
     *
     * @param view The {@link View} with {@link ViewPagerBottomSheetBehavior}.
     * @return The {@link ViewPagerBottomSheetBehavior} associated with the {@code view}.
     */
    @SuppressWarnings("unchecked")
    public static <V extends View> ViewPagerBottomSheetBehavior<V> from(V view) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (!(params instanceof CoordinatorLayout.LayoutParams)) {
            throw new IllegalArgumentException("The view is not a child of CoordinatorLayout");
        }
        CoordinatorLayout.Behavior behavior = ((CoordinatorLayout.LayoutParams) params).getBehavior();
        if (!(behavior instanceof ViewPagerBottomSheetBehavior)) {
            throw new IllegalArgumentException(
                    "The view is not associated with ViewPagerBottomSheetBehavior");
        }
        return (ViewPagerBottomSheetBehavior<V>) behavior;
    }
}