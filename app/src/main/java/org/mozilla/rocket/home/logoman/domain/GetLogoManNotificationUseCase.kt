package org.mozilla.rocket.home.logoman.domain

import androidx.lifecycle.LiveData
import org.mozilla.focus.telemetry.TelemetryWrapper
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

    sealed class Notification(
        val id: String,
        val title: String,
        val subtitle: String?,
        val imageUrl: String?,
        val action: LogoManAction?,
        val type: String?
    ) {
        class RemoteNotification(
            id: String,
            title: String,
            subtitle: String?,
            imageUrl: String?,
            action: LogoManAction.UriAction?,
            type: String?
        ) : Notification(id, title, subtitle, imageUrl, action, type)

        class MissionNotification(
            id: String,
            title: String,
            subtitle: String?,
            imageUrl: String?,
            action: LogoManAction.OpenMissionPage?,
            type: String?
        ) : Notification(id, title, subtitle, imageUrl, action, type)
    }

    sealed class LogoManAction {
        data class UriAction(val action: String) : LogoManAction()
        data class OpenMissionPage(val mission: Mission) : LogoManAction()

        fun getLink(): String? = when (this) {
            is UriAction -> action
            is OpenMissionPage -> mission.missionName
        }
    }
}

private fun Notification.toLogoManNotification() = GetLogoManNotificationUseCase.Notification.RemoteNotification(
            messageId,
            title,
            subtitle,
            imageUrl,
            action?.let { GetLogoManNotificationUseCase.LogoManAction.UriAction(it) },
            type
        )

private fun Mission.toLogoManNotification() = GetLogoManNotificationUseCase.Notification.MissionNotification(
            uniqueId,
            title,
            null,
            imageUrl,
            GetLogoManNotificationUseCase.LogoManAction.OpenMissionPage(mission = this),
            TelemetryWrapper.Extra_Value.REWARDS
        )