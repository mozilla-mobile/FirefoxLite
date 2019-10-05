package org.mozilla.rocket.msrp.data

import android.content.Context
import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.mozilla.rocket.extension.map
import org.mozilla.rocket.preference.stringLiveData
import org.mozilla.strictmodeviolator.StrictModeViolation

class MissionLocalDataSource(appContext: Context) {

    private val preference = StrictModeViolation.tempGrant({ builder ->
        builder.permitDiskReads()
    }, {
        appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    })

    fun getReadMissionIdsLiveData(): LiveData<List<String>> =
            preference.stringLiveData(KEY_READ_MISSIONS, "[]")
                    .map { parseReadMissionsString(it) }

    suspend fun addReadMissionId(id: String) = withContext(Dispatchers.IO) {
        val updatedIds = getReadMissionIds()
                .toMutableSet()
                .apply { add(id) }
                .toList()
        saveReadMissionIds(updatedIds)
    }

    private fun getReadMissionIds(): List<String> =
            parseReadMissionsString(
                requireNotNull(preference.getString(KEY_READ_MISSIONS, "[]"))
            )

    private fun saveReadMissionIds(ids: List<String>) {
        preference.edit().apply {
            putString(KEY_READ_MISSIONS, ids.toString())
        }.apply()
    }

    private fun parseReadMissionsString(idsStr: String): List<String> =
            mutableListOf<String>().apply {
                val jsonArray = JSONArray(idsStr)
                for (i in 0 until jsonArray.length()) {
                    add(jsonArray.getString(i))
                }
            }

    fun hasJoinedAnyMissionBefore(): Boolean =
            preference.getBoolean(KEY_HAS_JOINED_ANY_MISSION_BEFORE, false)

    fun setJoinedAnyMissionBefore() {
        preference.edit().apply {
            putBoolean(KEY_HAS_JOINED_ANY_MISSION_BEFORE, true)
        }.apply()
    }

    companion object {
        private const val PREF_NAME = "msrp"
        private const val KEY_READ_MISSIONS = "read_missions"
        private const val KEY_HAS_JOINED_ANY_MISSION_BEFORE = "has_joined_any_mission_before"
    }
}