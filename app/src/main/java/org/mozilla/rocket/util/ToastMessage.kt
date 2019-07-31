package org.mozilla.rocket.util

class ToastMessage(
    val stringResId: Int,
    val duration: Int = LENGTH_SHORT,
    vararg val args: String
) {
    companion object {
        const val LENGTH_SHORT = 0
        const val LENGTH_LONG = 1
    }
}