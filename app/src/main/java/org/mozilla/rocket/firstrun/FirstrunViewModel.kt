package org.mozilla.rocket.firstrun

import androidx.lifecycle.ViewModel
import org.mozilla.rocket.download.SingleLiveEvent

class FirstrunViewModel : ViewModel() {

    val finishFirstRunEvent = SingleLiveEvent<Unit>()

    fun onAnimationFinished() {
        finishFirstRunEvent.call()
    }
}