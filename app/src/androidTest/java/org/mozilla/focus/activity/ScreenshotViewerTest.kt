package org.mozilla.focus.activity

import android.Manifest
import android.content.Intent
import android.support.test.espresso.intent.rule.IntentsTestRule
import android.support.test.rule.GrantPermissionRule
import android.support.test.runner.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.autobot.screenshot
import org.mozilla.focus.autobot.session
import org.mozilla.focus.helper.BeforeTestTask
import org.mozilla.focus.screenshot.model.Screenshot

@RunWith(AndroidJUnit4::class)
class ScreenshotViewerTest {

    @JvmField
    @Rule
    val filePermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)

    @JvmField
    @Rule
    val intentsTestRule = IntentsTestRule(MainActivity::class.java, true, false)

    private lateinit var screenshot: Screenshot

    @Before
    fun setUp() {
        BeforeTestTask.Builder().build().execute()

        // Insert photo to database
        screenshot {
            screenshot = insertScreenshotToDatabase()
        }

        // Launch activity
        intentsTestRule.launchActivity(Intent())

        session {
            clickHomeMenu()
            clickMenuMyShots()
        }
    }

    @After
    fun tearDown() {

        screenshot {
            deleteScreenshotFromDatabase(screenshot)
        }
    }

    /**
     * Test case no: TC0057
     * Test case name: View photo info in screenshot viewer
     * Steps:
     * 1. Given pre-existing screenshot
     * 2. Tap menu -> my shots viewer -> first screenshot
     * 3. Click info
     * 4. Check image dialog is displayed
     * 5. press back
     * 6. Check image info dialog is dismissed
     * */
    @Test
    fun screenshotViewerInfo() {

        screenshot {
            clickFirstItemInMyShotsAndOpen()
            clickInfoTheFirstItemInMyShots()
            pressBack()
            checkScreenshotInfoDialogDimissed()
        }
    }

    /**
     * Test case no: TC0054
     * Test case name: Open web link in screenshot viewer
     * Steps:
     * 1. Given pre-existing screenshot
     * 2. Tap menu -> my shots viewer -> first screenshot
     * 3. Click open the web
     * 5. Check url matches
     * */
    @Test
    fun screenshotViewerOpenWeb() {

        screenshot {
            clickFirstItemInMyShotsAndOpen()
            clickOpenWebInScreenshotViewer(screenshot.url)
        }
    }

    /**
     * Test case no: TC0055
     * Test case name: Edit photo in screenshot viewer
     * Steps:
     * 1. Given pre-existing screenshot
     * 2. Tap menu -> my shots viewer -> first screenshot
     * 3. Click edit
     * 5. Check intent is sent
     * */
    @Test
    fun screenshotViewerEdit() {

        screenshot {
            clickFirstItemInMyShotsAndOpen()
            clickEditInScreenshotViewer()
        }
    }

    /**
     * Test case no: TC0054
     * Test case name: Share photo in screenshot viewer
     * Steps:
     * 1. Given pre-existing screenshot
     * 2. Tap menu -> my shots viewer -> first screenshot
     * 3. Click share
     * 5. Check intent is sent
     * */
    @Test
    fun screenshotViewerShare() {

        screenshot {
            clickFirstItemInMyShotsAndOpen()
            clickShareInScreenshotViewer()
        }
    }
}
