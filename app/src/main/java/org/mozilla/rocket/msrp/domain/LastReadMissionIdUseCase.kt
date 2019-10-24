package org.mozilla.rocket.msrp.domain

import androidx.lifecycle.LiveData
import org.mozilla.rocket.msrp.data.MissionRepository

class LastReadMissionIdUseCase(
    private val missionRepository: MissionRepository
) {

    operator fun invoke(): LiveData<String> = missionRepository.getLastReadNotificationIdLiveData()
}