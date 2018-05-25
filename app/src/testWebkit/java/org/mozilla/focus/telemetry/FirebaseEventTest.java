/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.telemetry;

import android.os.Bundle;
import android.support.annotation.NonNull;

import org.junit.Test;

import java.util.Arrays;

import static junit.framework.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FirebaseEventTest {

    final private static char FAKE_EVENT_NAME_CHAR = 'a';

    final private static String INVALID_EVENT_NAME;
    final private static String VALID_EVENT_NAME;

    final private static String INVALID_PARAM_NAME;
    final private static String VALID_PARAM_NAME;

    final private static String INVALID_PARAM_VALUE;
    final private static String VALID_PARAM_VALUE;

    final private static int EVENT_NAME_SEPARATOR_LENGTH = FirebaseEvent.EVENT_NAME_SEPARATOR.length() * 2;

    static {
        // event name already contains two "__", so they are 36 characters left
        INVALID_EVENT_NAME = prepareString(FirebaseEvent.MAX_LENGTH_EVENT_NAME - EVENT_NAME_SEPARATOR_LENGTH + 1);
        VALID_EVENT_NAME = prepareString(FirebaseEvent.MAX_LENGTH_EVENT_NAME - EVENT_NAME_SEPARATOR_LENGTH);

        INVALID_PARAM_NAME = prepareString(FirebaseEvent.MAX_LENGTH_PARAM_NAME + 1);
        VALID_PARAM_NAME = prepareString(FirebaseEvent.MAX_LENGTH_PARAM_NAME);

        INVALID_PARAM_VALUE = prepareString(FirebaseEvent.MAX_LENGTH_PARAM_VALUE + 1);
        VALID_PARAM_VALUE = prepareString(FirebaseEvent.MAX_LENGTH_PARAM_VALUE);

    }

    private static String prepareString(final int size) {
        char[] chars = new char[size];
        Arrays.fill(chars, FAKE_EVENT_NAME_CHAR);
        return new String(chars);
    }

    @NonNull
    private FirebaseEvent generateValidFirebaseEvent() {
        return FirebaseEvent.create("", "", "", VALID_EVENT_NAME);
    }

//    @Test(expected = IllegalArgumentException.class)
//    public void eventNameLongerThan40Characters_shouldThrowIllegalArgumentException() {
//        FirebaseEvent.create("", "", "", INVALID_EVENT_NAME);
//    }

    @Test
    public void eventNameValid_shouldBeSafe() {
        try {
            generateValidFirebaseEvent();
        } catch (Exception e) {
            fail("Should not have thrown any exception");
        }
    }


    @Test
    public void nullContext_shouldBeSafe() {
        try {
            generateValidFirebaseEvent().event(null);
        } catch (Exception e) {
            fail("Should not have thrown any exception");
        }
    }

//    @Test(expected = IllegalArgumentException.class)
//    public void paramSizeTooLarge_shouldThrow() {
//        final FirebaseEvent firebaseEvent = generateValidFirebaseEvent();
//        final Bundle params = mock(Bundle.class);
//        firebaseEvent.setParam(params);
//        when(params.size()).thenReturn(FirebaseEvent.MAX_PARAM_SIZE);
//        firebaseEvent.param("", "");
//    }

    @Test
    public void paramSizeValid_shouldBeSafe() {
        final FirebaseEvent firebaseEvent = generateValidFirebaseEvent();
        final Bundle extras = mock(Bundle.class);
        firebaseEvent.setParam(extras);

        final int validParamSize = FirebaseEvent.MAX_PARAM_SIZE - 1;
        when(extras.size()).thenReturn(validParamSize);
        try {
            firebaseEvent.param("", "");
        } catch (Exception e) {
            fail("Should not have thrown any exception");
        }
    }

//    @Test(expected = IllegalArgumentException.class)
//    public void paramKeyLengthTooLarge_shouldThrow() {
//        final FirebaseEvent firebaseEvent = generateValidFirebaseEvent();
//        firebaseEvent.param(INVALID_PARAM_NAME, "");
//    }

    @Test
    public void paramKeyValid_shouldBeSafe() {
        final FirebaseEvent firebaseEvent = generateValidFirebaseEvent();
        try {
            firebaseEvent.param(VALID_PARAM_NAME, "");
        } catch (Exception e) {
            fail("Should not have thrown any exception");
        }
    }

//    @Test(expected = IllegalArgumentException.class)
//    public void paramValueLengthTooLarge_shouldThrow() {
//        final FirebaseEvent firebaseEvent = generateValidFirebaseEvent();
//        firebaseEvent.param("", INVALID_PARAM_VALUE);
//    }

    @Test
    public void paramValueLengthValid_shouldBeSafe() {
        final FirebaseEvent firebaseEvent = generateValidFirebaseEvent();
        try {
            firebaseEvent.param("", VALID_PARAM_VALUE);
        } catch (Exception e) {
            fail("Should not have thrown any exception");
        }
    }


}