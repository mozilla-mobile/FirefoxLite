package org.mozilla.focus.screengrab

import org.junit.ClassRule

import tools.fastlane.screengrab.locale.LocaleTestRule

/**
 * Base class for tests that take screenshots.
 */
open class BaseScreenshot {
    companion object {

        @JvmField
        @ClassRule
        val localeTestRule = LocaleTestRule()
    }

}
