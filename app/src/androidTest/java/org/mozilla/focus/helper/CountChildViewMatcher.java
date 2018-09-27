package org.mozilla.focus.helper;

import androidx.test.espresso.matcher.BoundedMatcher;
import android.view.View;
import android.view.ViewGroup;

import org.hamcrest.Description;
import org.hamcrest.Matcher;

public class CountChildViewMatcher {

    public static Matcher<View> withChildViewCount(final int count, final Matcher<View> childMatcher) {
        return new BoundedMatcher<View, ViewGroup>(ViewGroup.class) {
            @Override
            protected boolean matchesSafely(ViewGroup viewGroup) {
                int matchCount = 0;
                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    if (childMatcher.matches(viewGroup.getChildAt(i))) {
                        matchCount++;
                    }
                }

                return (matchCount > 0 && matchCount <= count);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("ViewGroup with child-count = " + count);
                childMatcher.describeTo(description);
            }
        };
    }
}