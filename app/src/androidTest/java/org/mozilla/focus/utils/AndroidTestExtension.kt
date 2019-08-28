package org.mozilla.focus.utils

import android.view.View
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matcher
import org.hamcrest.Matchers

fun visibleWithId(resId: Int): Matcher<View> =
        Matchers.allOf(ViewMatchers.withId(resId), ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))