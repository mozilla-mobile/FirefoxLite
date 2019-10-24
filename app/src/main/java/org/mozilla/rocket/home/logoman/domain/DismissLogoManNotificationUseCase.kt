package org.mozilla.rocket.home.logoman.domain

import org.mozilla.rocket.home.logoman.data.LogoManNotificationRepo
import org.mozilla.rocket.home.logoman.ui.LogoManNotification
import org.mozilla.rocket.home.logoman.ui.LogoManNotification.Notification.MissionNotification
import org.mozilla.rocket.home.logoman.ui.LogoManNotification.Notification.RemoteNotification
import org.mozilla.rocket.msrp.data.MissionRepository

class DismissLogoManNotificationUseCase(
    private val logoManNotificationRepo: LogoManNotificationRepo,
    private val missionRepo: MissionRepository
) {

    operator fun invoke(notification: LogoManNotification.Notification) {
        when (notification) {
            is RemoteNotification -> logoManNotificationRepo.saveLastReadNotificationId(notification.id)
            is MissionNotification -> missionRepo.saveLastReadNotificationId(notification.id)
        }
    }
}