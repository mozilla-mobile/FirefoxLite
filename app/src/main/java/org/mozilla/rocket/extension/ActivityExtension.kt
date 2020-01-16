package org.mozilla.rocket.extension

import android.app.Activity
import android.content.Intent

fun Activity.isLaunchedFromHistory(): Boolean =
        intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY == Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY