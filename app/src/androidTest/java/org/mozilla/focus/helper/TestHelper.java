/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mo
 * This Source Code Form is subject to the terms of the Mozilla Pub
 * License, v. 2.0. If a copy of the MPL was not distributed with t
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.helper;

import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.uiautomator.UiDevice;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.io.IOException;
import java.io.InputStream;

import okio.Buffer;
import okio.Okio;

import static android.support.test.internal.util.Checks.checkNotNull;


public class TestHelper {

    public static UiDevice DEVICE =  UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

    public static Buffer readTestAsset(String filename) throws IOException {
        try (final InputStream stream = InstrumentationRegistry.getContext().getAssets().open(filename)) {
            return readStreamFile(stream);
        }
    }

    public static Buffer readStreamFile(InputStream file) throws IOException {

        Buffer buffer = new Buffer();
        buffer.writeAll(Okio.source(file));
        return buffer;
    }

    public static Matcher<View> atPosition(final int position, @NonNull final Matcher<View> itemMatcher) {
        checkNotNull(itemMatcher);
        return new BoundedMatcher<View, RecyclerView>(RecyclerView.class) {
            @Override
            public void describeTo(Description description) {
                description.appendText("has item at position " + position + ": ");
                itemMatcher.describeTo(description);
            }

            @Override
            protected boolean matchesSafely(final RecyclerView view) {
                RecyclerView.ViewHolder viewHolder = view.findViewHolderForAdapterPosition(position);
                if (viewHolder == null) {
                    // has no item on such position
                    return false;
                }
                return itemMatcher.matches(viewHolder.itemView);
            }
        };
    }
}
