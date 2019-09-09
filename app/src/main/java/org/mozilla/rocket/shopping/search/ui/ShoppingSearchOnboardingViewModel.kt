package org.mozilla.rocket.shopping.search.ui

import androidx.lifecycle.ViewModel
import org.mozilla.rocket.download.SingleLiveEvent

class ShoppingSearchOnboardingViewModel : ViewModel() {

    val dismissEvent = SingleLiveEvent<Unit>()
}