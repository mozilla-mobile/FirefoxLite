package org.mozilla.focus.utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class AppConstantsTest {

    @Test
    public void getCorrectChannel() {
        assertEquals("It's a debug build", AppConstants.BUILD_TYPE_DEBUG, AppConstants.getChannel());
    }

}