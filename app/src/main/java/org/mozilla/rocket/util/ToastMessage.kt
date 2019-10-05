package org.mozilla.rocket.util

class ToastMessage {

    val message: String?
    val stringResId: Int?
    val duration: Int
    val args: Array<out String>

    constructor(
        stringResId: Int,
        duration: Int = LENGTH_SHORT,
        vararg args: String
    ) {
        this.message = null
        this.stringResId = stringResId
        this.duration = duration
        this.args = args
    }

    constructor(
        message: String,
        duration: Int = LENGTH_SHORT
    ) {
        this.message = message
        this.stringResId = null
        this.duration = duration
        this.args = emptyArray()
    }

    companion object {
        const val LENGTH_SHORT = 0
        const val LENGTH_LONG = 1
    }
}