package org.mozilla.focus.helper;

import androidx.test.espresso.matcher.BoundedMatcher;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import static androidx.test.internal.util.Checks.checkNotNull;

public class DecodedTextMatcher {
    private static final String TAG = "DecodedTextMatcher";

    public static Matcher<View> withText(final Matcher<String> stringMatcher) {
        checkNotNull(stringMatcher);
        return new BoundedMatcher<View, TextView>(TextView.class) {
            @Override
            public void describeTo(Description description) {
                description.appendText("with decoded text ");
                stringMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(TextView textView) {
                try {
                    final String decoded = URLDecoder.decode(textView.getText().toString(), "UTF-8");
                    return stringMatcher.matches(decoded);
                } catch (UnsupportedEncodingException e) {
                    Log.e(TAG, "Error when matchesSafely: " + e);
                    return false;
                }
            }
        };
    }
}