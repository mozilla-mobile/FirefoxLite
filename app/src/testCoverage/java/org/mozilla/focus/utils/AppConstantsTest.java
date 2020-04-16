package org.mozilla.focus.utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class AppConstantsTest {

    @Test
    public void getCorrectChannel() {
        assertEquals("It's a Coverage build", AppConstants.BUILD_TYPE_COVERAGE, AppConstants.getChannel());
    }

}