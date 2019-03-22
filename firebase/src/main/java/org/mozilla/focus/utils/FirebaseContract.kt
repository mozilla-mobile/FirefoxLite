package org.mozilla.focus.utils

import android.content.Context
import android.os.Bundle
import android.support.annotation.WorkerThread

interface FirebaseContract {

    val fcmToken: String?

    fun prettify(string: String): String

    fun getRcString(context: Context, key: String): String

    fun getRcLong(context: Context, key: String): Long

    fun getRcBoolean(context: Context, key: String): Boolean

    @WorkerThread
    fun deleteInstanceId()

    fun enableCloudMessaging(context: Context, componentName: String, enable: Boolean)

    fun enableAnalytics(context: Context, enable: Boolean)

    fun enableRemoteConfig(
        context: Context, callback: FirebaseWrapper.RemoteConfigFetchCallback
    )

    fun setDeveloperModeEnabled(developerModeEnabled: Boolean)

    fun event(context: Context?, key: String, param: Bundle?)
}