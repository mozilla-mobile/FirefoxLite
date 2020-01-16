package org.mozilla.rocket.content.news.data.dailyhunt

import android.content.Context
import org.json.JSONException
import org.json.JSONObject
import org.mozilla.focus.utils.FirebaseHelper
import org.mozilla.strictmodeviolator.StrictModeViolation
import java.util.UUID

data class DailyHuntProvider(
    val apiKey: String,
    val secretKey: String,
    val partnerCode: String,
    val isEnableFromRemote: Boolean,
    val userId: String
) {

    fun shouldEnable(appContext: Context): Boolean {
        val preference =
            StrictModeViolation.tempGrant({ builder ->
                builder.permitDiskReads()
            }, {
                appContext.getSharedPreferences(NEWS_SETTING_PREF_NAME, Context.MODE_PRIVATE)
            })
        val isEnabledByUser = preference.getBoolean(KEY_BOOL_IS_USER_ENABLED_PERSONALIZED_NEWS, false)
        return isEnabledByUser && isEnableFromRemote
    }

    companion object {
        private const val DAILY_HUNT_PREF_NAME = "daily_hunt"
        private const val KEY_STR_USER_ID = "user_id"
        private const val NEWS_SETTING_PREF_NAME = "news_settings"
        private const val KEY_BOOL_IS_USER_ENABLED_PERSONALIZED_NEWS = "is_user_enabled_personalized_news"

        fun getProvider(appContext: Context): DailyHuntProvider? {
            val config = FirebaseHelper.getFirebase().getRcString("str_dailyhunt_provider")
            try {
                val jsonObject = JSONObject(config)
                val apiKey = jsonObject.optString("api_key")
                val secretKey = jsonObject.optString("secret_key")
                val partnerCode = jsonObject.optString("partner_code")
                val isEnable = jsonObject.optBoolean("isEnable")
                val userId = getUserId(appContext)

                return DailyHuntProvider(apiKey, secretKey, partnerCode, isEnable, userId)
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            return null
        }

        private fun getUserId(appContext: Context): String {
            val preference =
                StrictModeViolation.tempGrant({ builder ->
                    builder.permitDiskReads()
                }, {
                    appContext.getSharedPreferences(DAILY_HUNT_PREF_NAME, Context.MODE_PRIVATE)
                })

            val cachedUserId = preference.getString(KEY_STR_USER_ID, "") ?: ""
            return if (cachedUserId.isEmpty()) {
                UUID.randomUUID().toString().also {
                    preference.edit().putString(KEY_STR_USER_ID, it).apply()
                }
            } else {
                cachedUserId
            }
        }
    }
}