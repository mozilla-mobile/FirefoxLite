package org.mozilla.focus.autobot

import android.content.Intent
import android.support.test.InstrumentationRegistry
import android.support.test.uiautomator.UiDevice
import android.support.test.uiautomator.UiObject
import android.support.test.uiautomator.UiObjectNotFoundException
import android.support.test.uiautomator.UiSelector
import org.junit.Assert
import org.mozilla.rocket.privately.PrivateModeActivity

inline fun notification(func: NotificationRobot.() -> Unit) = NotificationRobot().apply(func)
class NotificationRobot {

    private var uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    fun checkNotificationNotDisplayed(activity: PrivateModeActivity, msg: Int) {
        val notificationTextExist = uiDevice.findObject(UiSelector().text(activity.resources.getString(msg))).exists()
        Assert.assertFalse(notificationTextExist)
    }

    fun checkNotificationDisplayed(activity: PrivateModeActivity, msg: Int): UiObject? {
        var notificationObject = uiDevice.findObject(UiSelector().text(activity.resources.getString(msg)))
        return notificationObject
    }

    fun openNotification() {
        uiDevice.openNotification()
    }

    fun clickNotificationWithText(activity: PrivateModeActivity, msg: Int) {
        var notificationObject = checkNotificationDisplayed(activity, msg)

        try {
            notificationObject?.click()
        } catch (e: UiObjectNotFoundException) {
            e.printStackTrace()
        }
    }
    fun dismissNotification(activity: PrivateModeActivity) {
        val closeIntent = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        activity.sendBroadcast(closeIntent)
    }
}