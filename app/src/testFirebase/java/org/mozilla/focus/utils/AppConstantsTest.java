package org.mozilla.focus.utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class AppConstantsTest {

    @Test
    public void getCorrectChannel() {
        assertEquals("It's a Firebase build", AppConstants.BUILD_TYPE_FIREBASE, AppConstants.getChannel());
    }

}