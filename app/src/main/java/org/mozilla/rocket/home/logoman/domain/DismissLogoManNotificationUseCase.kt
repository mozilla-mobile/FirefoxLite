package org.mozilla.rocket.home.logoman.domain

import org.mozilla.rocket.home.logoman.data.LogoManNotificationRepo
import org.mozilla.rocket.home.logoman.ui.LogoManNotification

class DismissLogoManNotificationUseCase(private val logoManNotificationRepo: LogoManNotificationRepo) {

    operator fun invoke(notification: LogoManNotification.Notification) {
        logoManNotificationRepo.saveLastReadNotificationId(notification.id)
    }
}