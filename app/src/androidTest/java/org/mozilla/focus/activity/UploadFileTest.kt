/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.support.test.InstrumentationRegistry.getInstrumentation
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.pressImeActionButton
import android.support.test.espresso.action.ViewActions.replaceText
import android.support.test.espresso.intent.rule.IntentsTestRule
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.rule.GrantPermissionRule
import android.support.test.runner.AndroidJUnit4
import android.support.test.uiautomator.UiDevice
import android.support.test.uiautomator.UiObjectNotFoundException
import android.support.test.uiautomator.UiSelector
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.R
import org.mozilla.focus.helper.BeforeTestTask
import org.mozilla.focus.utils.AndroidTestUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class UploadFileTest {

    private var webServer: MockWebServer? = null

    @Rule
    @JvmField
    var write_permissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    @Rule
    @JvmField
    var read_permissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE)

    private lateinit var uiDevice: UiDevice

    private lateinit var context: Context

    private lateinit var testDir: File

    @Rule
    @JvmField
    var activityRule: IntentsTestRule<MainActivity> = object : IntentsTestRule<MainActivity>(MainActivity::class.java, true, false) {
        override fun beforeActivityLaunched() {
            super.beforeActivityLaunched()

            webServer = MockWebServer()

            try {
                webServer!!.enqueue(MockResponse()
                        .setBody(AndroidTestUtils.readTestAsset("upload_test.html"))
                        .addHeader("Set-Cookie", "sphere=battery; Expires=Wed, 21 Oct 2035 07:28:00 GMT;"))
                // TODO: Below response are reserved for future download tests
                webServer!!.start()
            } catch (e: IOException) {
                throw AssertionError("Could not start web server", e)
            }
        }

        override fun afterActivityFinished() {
            super.afterActivityFinished()

            try {
                webServer!!.close()
                webServer!!.shutdown()
            } catch (e: IOException) {
                throw AssertionError("Could not stop web server", e)
            }
        }
    }

    @Before
    fun setUp() {
        BeforeTestTask.Builder()
                .build()
                .execute()
    }

    @After
    fun tearDown() {

        // Delete test diretory on storage
        testDir.deleteRecursively()
    }

    /**
     * Test case no: TC0024
     * Test case name: Upload a file
     * Steps:
     * 1. Launch app
     * 2. Visit website with upload service
     * 3. Tap upload button
     * 4. Choose File from Documents app
     * 5. Check file upload successfully by checking filename is shown
     * Note : Journey to choose file from app will differ from different emulators.
     * Step 4 is hard code here so it only fits Nexus 5X.
     */
    @Test
    @Throws(InterruptedException::class, UiObjectNotFoundException::class, IOException::class)
    fun uploadFile() {

        // Generate image on exteranl storage
        generateImage()

        // Launch app
        context = activityRule.launchActivity(Intent())

        // Click and prepare to enter the URL
        onView(withId(R.id.home_fragment_fake_input)).perform(click())

        // Enter URL and load the page
        onView(withId(R.id.url_edit)).perform(replaceText(webServer!!.url(TEST_PATH).toString()), pressImeActionButton())

        // Tap webview upoload button
        uiDevice = UiDevice.getInstance(getInstrumentation())
        uiDevice.findObject(UiSelector().resourceId(HTML_ELEMENT_ID_UPLOAD)).click()

        openInternalStorage()

        // Tap folder name -> file name
        uiDevice.findObject(UiSelector().textContains(FOLDER_NAME)).click()
        uiDevice.findObject(UiSelector().textContains(FILE_NAME)).click()

        // Check file uploaded successfully
        uiDevice.findObject(UiSelector().textContains(FILE_NAME))
    }

    private fun openInternalStorage() {
        // Tap Documents app to open storage
        uiDevice.findObject(UiSelector().text("Documents")).click()

        // Tap More options
        uiDevice.findObject(UiSelector().descriptionContains("More options")).click()

        // Tap Show internal storage -> Show roots -> Android SDK buit x86
        // Note: internal storage name will vary on same emulator so only verify text contains "x86"
        uiDevice.findObject(UiSelector().textContains("Show internal storage")).click()
        uiDevice.findObject(UiSelector().description("Show roots")).click()
        uiDevice.findObject(UiSelector().textContains("x86")).click()
    }

    private fun generateImage() {

        val root = Environment.getExternalStorageDirectory().toString()
        testDir = File("$root/" + FOLDER_NAME)
        if (!testDir.exists()) {
            testDir.mkdirs()
        }
        val file = File(testDir, FILE_NAME)
        if (file.exists())
            file.delete()
        try {
            val out = FileOutputStream(file)
            out.flush()
            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private val TEST_PATH = "/"
        private var FOLDER_NAME = "saved_image"
        private var FILE_NAME = "Image.jpg"
        val HTML_ELEMENT_ID_UPLOAD = "upload"
    }
}
