package org.mozilla.rocket.helper

import android.content.Context
import org.mozilla.focus.utils.NoRemovableStorageException
import org.mozilla.focus.utils.StorageUtils
import java.io.File

class StorageHelper(context: Context) {
    private val appContext = context.applicationContext

    fun getAppMediaDirOnRemovableStorage(): File? =
            StorageUtils.getAppMediaDirOnRemovableStorage(appContext)

    fun getTargetDirOnRemovableStorageForDownloads(type: String): File? =
            StorageUtils.getTargetDirOnRemovableStorageForDownloads(appContext, type)

    fun hasRemovableStorage(): Boolean = try {
        getTargetDirOnRemovableStorageForDownloads("*/*") != null
    } catch (e: NoRemovableStorageException) {
        false
    }

    fun getTargetDirForSaveScreenshot(): File? =
            StorageUtils.getTargetDirForSaveScreenshot(appContext)
}