package org.mozilla.rocket.helper

import android.content.Context
import org.mozilla.focus.utils.StorageUtils
import java.io.File

class StorageHelper(context: Context) {
    private val appContext = context.applicationContext

    fun getAppMediaDirOnRemovableStorage(): File? =
            StorageUtils.getAppMediaDirOnRemovableStorage(appContext)

    fun getTargetDirOnRemovableStorageForDownloads(type: String): File? =
            StorageUtils.getTargetDirOnRemovableStorageForDownloads(appContext, type)

    fun getTargetDirForSaveScreenshot(): File? =
            StorageUtils.getTargetDirForSaveScreenshot(appContext)
}