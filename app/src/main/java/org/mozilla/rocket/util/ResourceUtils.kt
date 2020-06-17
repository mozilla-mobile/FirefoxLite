package org.mozilla.rocket.util

import android.content.res.Resources
import android.view.View

object ResourceUtils {
    fun Resources.getVisibility(id: Int): Int {
        return when (val flag = getInteger(id)) {
            0 -> View.VISIBLE
            1 -> View.INVISIBLE
            2 -> View.GONE
            else -> error("invalid visibility flag value: $flag")
        }
    }
}