package org.mozilla.rocket.util

import android.content.Context
import android.content.res.Resources
import androidx.annotation.RawRes

object AssetsUtils {

    fun loadStringFromRawResource(context: Context, @RawRes resId: Int): String? = try {
        context.resources.openRawResource(resId).bufferedReader().use { it.readText() }
    } catch (e: Resources.NotFoundException) {
        e.printStackTrace()
        null
    }
}