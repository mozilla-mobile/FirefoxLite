package org.mozilla.focus.utils

import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ListView
import android.widget.ScrollView
import androidx.core.widget.NestedScrollView
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matcher
import org.hamcrest.Matchers

fun visibleWithId(resId: Int): Matcher<View> =
        Matchers.allOf(ViewMatchers.withId(resId), ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))

class NestedScrollViewExtension(action: ViewAction) : ViewAction by action {
    override fun getConstraints(): Matcher<View> {
        return Matchers.allOf(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),
                ViewMatchers.isDescendantOfA(Matchers.anyOf(ViewMatchers.isAssignableFrom(NestedScrollView::class.java),
                        ViewMatchers.isAssignableFrom(ScrollView::class.java),
                        ViewMatchers.isAssignableFrom(HorizontalScrollView::class.java),
                        ViewMatchers.isAssignableFrom(ListView::class.java))))
    }
}