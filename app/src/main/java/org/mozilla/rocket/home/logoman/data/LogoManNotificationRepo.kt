package org.mozilla.rocket.home.logoman.data

import android.content.Context
import org.json.JSONException
import org.json.JSONObject
import org.mozilla.focus.utils.FirebaseHelper
import org.mozilla.strictmodeviolator.StrictModeViolation

class LogoManNotificationRepo(appContext: Context) {

    private val preference = StrictModeViolation.tempGrant({ builder ->
        builder.permitDiskReads()
    }, {
        appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    })

    fun getNotification(): Notification? =
            FirebaseHelper.getFirebase().getRcString(STR_LOGO_MAN_NOTIFICATION)
                    .takeIf { it.isNotEmpty() }
                    ?.jsonStringToNotification()
                    ?.takeIf { it.serialNumber != getLastReadNotificationId() }

    private fun getLastReadNotificationId(): Long =
            preference.getLong(SHARED_PREF_KEY_READ_NOTIFICATIONS, -1L)

    fun saveLastReadNotificationId(readId: Long) {
        preference.edit().putLong(SHARED_PREF_KEY_READ_NOTIFICATIONS, readId).apply()
    }

    companion object {
        private const val STR_LOGO_MAN_NOTIFICATION = "str_logo_man_notification"

        private const val PREF_NAME = "logo_man_notification"
        private const val SHARED_PREF_KEY_READ_NOTIFICATIONS = "shared_pref_key_read_notifications"
    }
}

data class Notification(
    val serialNumber: Long,
    val title: String,
    val subtitle: String
)

private fun String.jsonStringToNotification(): Notification? {
    return try {
        val jsonObject = JSONObject(this)
        Notification(
            jsonObject.getLong("serialNumber"),
            jsonObject.getString("title"),
            jsonObject.getString("subtitle")
        )
    } catch (e: JSONException) {
        e.printStackTrace()
        null
    }
}