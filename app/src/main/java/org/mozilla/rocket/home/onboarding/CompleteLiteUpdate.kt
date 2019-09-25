package org.mozilla.rocket.home.onboarding

import android.content.Context
import org.mozilla.focus.utils.NewFeatureNotice

class CompleteLiteUpdate(private val context: Context) {
    operator fun invoke() {
        NewFeatureNotice.getInstance(context).setLiteUpdateDidShow()
    }
}