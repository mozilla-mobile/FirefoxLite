package org.mozilla.focus.helper;

import android.view.View;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/** CustomViewMatcher is a help class to collect all customize view matchers that Espresso doesn't support **/
public class CustomViewMatcher {

    public static Matcher<View> isActivate() {
        return new IsActivateMatcher();
    }

    static final class IsActivateMatcher extends TypeSafeMatcher<View> {
        private IsActivateMatcher() {}

        @Override
        public void describeTo(Description description) {
            description.appendText("is activate");
        }

        @Override
        public boolean matchesSafely(View view) {
            return view.isActivated();
        }
    }

}
