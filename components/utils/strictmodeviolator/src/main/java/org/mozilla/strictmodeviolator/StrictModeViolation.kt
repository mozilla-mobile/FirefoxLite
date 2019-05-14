package org.mozilla.strictmodeviolator

import android.os.StrictMode

object StrictModeViolation {
    @JvmStatic
    fun <T> tempGrant(
        tempChange: (StrictMode.ThreadPolicy.Builder) -> StrictMode.ThreadPolicy.Builder,
        lambda: () -> T
    ): T {
        val cachedPolicy = StrictMode.getThreadPolicy()
        StrictMode.setThreadPolicy(tempChange(StrictMode.ThreadPolicy.Builder(cachedPolicy)).build())
        try {
            return lambda()
        } finally {
            StrictMode.setThreadPolicy(cachedPolicy)
        }
    }
}