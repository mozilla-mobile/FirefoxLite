package org.mozilla.rocket.home.logoman.domain

import org.mozilla.rocket.home.logoman.data.LogoManNotificationRepo
import org.mozilla.rocket.home.logoman.ui.LogoManNotification
import org.mozilla.rocket.msrp.data.MissionRepository

class DismissLogoManNotificationUseCase(
    private val logoManNotificationRepo: LogoManNotificationRepo,
    private val missionRepo: MissionRepository
) {

    operator fun invoke(notification: LogoManNotification.Notification) {
        logoManNotificationRepo.saveLastReadNotificationId(notification.id)
        missionRepo.saveLastReadNotificationId(notification.id)
    }
}