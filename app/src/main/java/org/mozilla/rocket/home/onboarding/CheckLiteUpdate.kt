package org.mozilla.rocket.home.onboarding

import android.content.Context
import org.mozilla.focus.utils.NewFeatureNotice

class CheckLiteUpdate(private val context: Context) {
    operator fun invoke(): Boolean {
        return !NewFeatureNotice.getInstance(context).shouldShowLiteUpdate()
    }
}