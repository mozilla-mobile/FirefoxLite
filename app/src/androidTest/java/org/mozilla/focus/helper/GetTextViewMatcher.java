package org.mozilla.focus.helper;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import android.view.View;
import android.widget.TextView;

import org.hamcrest.Matcher;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;

public class GetTextViewMatcher {

    public static String getText(final Matcher<View> matcher) {
        final String stringHolder[] = { null };
        onView(matcher).perform(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(TextView.class);
            }

            @Override
            public String getDescription() {
                return "getting text from a TextView";
            }

            @Override
            public void perform(UiController uiController, View view) {
                TextView tv = (TextView) view;
                stringHolder[0] = tv.getText().toString();
            }
        });
        return stringHolder[0];
    }
}