package org.mozilla.rocket.msrp.domain

import org.mozilla.rocket.download.SingleLiveEvent
import org.mozilla.rocket.msrp.data.MissionRepository

class GetContentHubClickOnboardingEventUseCase(
    private val missionRepository: MissionRepository
) {

    operator fun invoke(): SingleLiveEvent<String> = missionRepository.getContentHubClickOnboardingEvent()
}