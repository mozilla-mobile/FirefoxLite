package org.mozilla.rocket.shopping.search.ui

import androidx.lifecycle.ViewModel
import org.mozilla.rocket.download.SingleLiveEvent

class ShoppingSearchContentSwitchOnboardingViewModel : ViewModel() {

    val dismissEvent = SingleLiveEvent<Unit>()
}