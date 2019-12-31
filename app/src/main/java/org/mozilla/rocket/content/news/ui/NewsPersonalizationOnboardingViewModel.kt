package org.mozilla.rocket.content.news.ui

import android.util.Log
import androidx.lifecycle.ViewModel

class NewsPersonalizationOnboardingViewModel() : ViewModel() {

    fun onPersonalizationSelected(bool: Boolean) {
        // TODO: setup personalization
        Log.d("xx", "Personalization: $bool")
    }
}