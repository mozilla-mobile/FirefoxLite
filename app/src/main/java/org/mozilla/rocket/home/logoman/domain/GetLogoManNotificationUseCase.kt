package org.mozilla.rocket.home.logoman.domain

import androidx.lifecycle.LiveData
import org.mozilla.rocket.extension.combineLatest
import org.mozilla.rocket.extension.map
import org.mozilla.rocket.home.logoman.data.LogoManNotificationRepo
import org.mozilla.rocket.home.logoman.data.Notification
import org.mozilla.rocket.msrp.data.Mission
import org.mozilla.rocket.msrp.data.MissionRepository

class GetLogoManNotificationUseCase(
    private val logoManNotificationRepo: LogoManNotificationRepo,
    private val missionRepository: MissionRepository
) {

    operator fun invoke(): LiveData<Notification?> =
            combineLatest(missionRepository.getNotificationMission(), logoManNotificationRepo.getNotification())
                    .map { (mission, notification) ->
                        return@map mission?.toLogoManNotification() ?: notification?.toLogoManNotification()
                    }

    data class Notification(
        val id: String,
        val title: String,
        val subtitle: String?,
        val action: LogoManAction?
    )

    sealed class LogoManAction {
        data class OpenMissionPage(val mission: Mission) : LogoManAction()
    }
}

private fun Notification.toLogoManNotification() = GetLogoManNotificationUseCase.Notification(
            serialNumber.toString(),
            title,
            subtitle,
            null
        )

private fun Mission.toLogoManNotification() = GetLogoManNotificationUseCase.Notification(
            uniqueId,
            title,
            null,
            GetLogoManNotificationUseCase.LogoManAction.OpenMissionPage(mission = this)
        )