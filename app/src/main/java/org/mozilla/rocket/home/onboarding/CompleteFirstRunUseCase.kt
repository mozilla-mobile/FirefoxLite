package org.mozilla.rocket.home.onboarding

import android.content.Context
import org.mozilla.focus.utils.NewFeatureNotice

class CompleteFirstRunUseCase(private val context: Context) {
    operator fun invoke() {
        NewFeatureNotice.getInstance(context).setFirstRunDidShow()
    }
}