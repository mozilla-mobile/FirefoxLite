package org.mozilla.rocket.home.logoman.data

import android.content.Context
import androidx.lifecycle.LiveData
import org.json.JSONException
import org.json.JSONObject
import org.mozilla.focus.utils.FirebaseHelper
import org.mozilla.rocket.extension.map
import org.mozilla.rocket.preference.stringLiveData
import org.mozilla.strictmodeviolator.StrictModeViolation

class LogoManNotificationRepo(appContext: Context) {

    private val preference = StrictModeViolation.tempGrant({ builder ->
        builder.permitDiskReads()
    }, {
        appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    })

    fun getNotification(): LiveData<Notification?> =
            getLastReadNotificationId().map { lastReadId ->
                FirebaseHelper.getFirebase().getRcString(STR_LOGO_MAN_NOTIFICATION)
                        .takeIf { it.isNotEmpty() }
                        ?.jsonStringToNotification()
                        ?.takeIf { it.serialNumber.toString() != lastReadId }
            }

    private fun getLastReadNotificationId(): LiveData<String> =
            preference.stringLiveData(SHARED_PREF_KEY_READ_NOTIFICATION_ID, "")

    fun saveLastReadNotificationId(readId: String) {
        preference.edit().putString(SHARED_PREF_KEY_READ_NOTIFICATION_ID, readId).apply()
    }

    companion object {
        private const val STR_LOGO_MAN_NOTIFICATION = "str_logo_man_notification"

        private const val PREF_NAME = "logo_man_notification"
        private const val SHARED_PREF_KEY_READ_NOTIFICATION_ID = "shared_pref_key_read_notification_id"
    }
}

data class Notification(
    val serialNumber: Long,
    val title: String,
    val subtitle: String?
)

private fun String.jsonStringToNotification(): Notification? {
    return try {
        val jsonObject = JSONObject(this)
        Notification(
            jsonObject.getLong("serialNumber"),
            jsonObject.getString("title"),
            jsonObject.optString("subtitle", null)
        )
    } catch (e: JSONException) {
        e.printStackTrace()
        null
    }
}