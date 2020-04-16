package org.mozilla.focus.utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class AppConstantsTest {

    @Test
    public void getCorrectChannel() {
        if (AppConstants.isNightlyBuild()) {
            assertEquals("It's a Nightly build", AppConstants.FLAVOR_product_NIGHTLY, AppConstants.getChannel());
        } else {
            assertEquals("It's a Release build", AppConstants.BUILD_TYPE_RELEASE, AppConstants.getChannel());
        }
    }
}