package org.mozilla.rocket.deeplink.task

import android.content.Context

interface Task {
    fun execute(context: Context)
}